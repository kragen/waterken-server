// Copyright 2007 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Failure.maxEntitySize;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Equatable;
import org.joe_e.Immutable;
import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.joe_e.Token;
import org.joe_e.array.ConstArray;
import org.joe_e.array.ImmutableArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.db.Database;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.Service;
import org.waterken.db.Transaction;
import org.waterken.http.Client;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.Stream;
import org.waterken.io.limited.Limited;
import org.waterken.io.limited.TooBig;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * Manages a pending request queue for a peer vat.
 */
/* package */ final class
Pipeline implements Equatable, Serializable {
    static private final long serialVersionUID = 1L;

    protected final String peer;                    // absolute URI of peer vat
    protected final String key;                     // messaging session key
    protected final String name;                    // messaging session name
    private   final Receiver<Promise<?>> enqueue;
    private   final Receiver<Effect<Server>> effect;
    private   final Promise<Outbound> outbound;

    private         ConstArray<List<Operation>> pollers = ConstArray.array();

    private         ConstArray<Object> returns; // active window returns
    private         long activeWindow = 1;    // on-the-air window id
    private         int  priorIndex = -1;     // last acknowledged window index
    private         long acknowledged = 0;    // total acknowledged operations

    private   final List<Operation> pending = List.list();
    private         long halts = 0;     // number of pending pipeline flushes
    private         long queries = 0;   // number of queries after last flush
    private         int trailingIndex = -1; // last queued window index

    /*
     * Message sending is halted before an update that follows a query. Sending
     * resumes once the response to the query has been received.
     */

    protected
    Pipeline(final String peer, final String key, final String name,
             final Receiver<Promise<?>> enqueue,
             final Receiver<Effect<Server>> effect,
             final Promise<Outbound> outbound) {
        this.peer = peer;
        this.key = key;
        this.name = name;
        this.enqueue = enqueue;
        this.effect = effect;
        this.outbound = outbound;
    }

    /**
     * Gets the id of the currently active window.
     */
    protected long
    getActiveWindow() { return activeWindow; }

    /**
     * Is the specified window still open for pipelined messages?
     */
    protected boolean
    canPipeline(final long window) {
        return 0 == queries && window == activeWindow + halts;
    }

    /**
     * Get a prior return value in the active window.
     */
    protected Object
    follow(final int message) { return returns.get(message); }

    /**
     * Append a return value to the active window.
     */
    protected void
    lead(final int message, final Object value) {
        if (message != returns.length()) { throw new Error(); }
        returns = returns.with(value);
    }

    /**
     * Serializes requests and enqueues them on the transient HTTP connection.
     * @param max       maximum number of requests to enqueue
     * @param skipTo    mid of first request to send
     */
    protected ImmutableArray<Effect<Server>>
    restart(final long max, final long skipTo) {
        final ImmutableArray.Builder<Effect<Server>> r=ImmutableArray.builder();
        int index = priorIndex;
        boolean queried = false;
        long sent = acknowledged;
        boolean found = false;
        long n = max;
        for (final Operation x : pending) {
            if (x.isUpdate) {
                if (queried) { break; }
                index += 1;
            }
            if (x.isQuery) { queried = true; }
            final long mid = sent++;
            found = found || skipTo == mid;
            if (!found) { continue; }

            if (!(x instanceof Task)) {
                final Position position = x.isUpdate ?
                    new Position(activeWindow, index,
                                 name + "-" + activeWindow + "-" + index) :
                    new Position(0, 0, name + "-0-" + mid);
                try {
                    r.append(fulfill(peer, position, mid,
                                     x.render(key, activeWindow, index)));
                } catch (final Exception reason) {
                    r.append(reject(peer, position, mid, reason));
                }
            }

            if (0 == --n) { break; }
        }
        return r.snapshot();
    }

    /**
     * Creates a query transaction to serialize a request.
     */
    static private Effect<Server>
    restartTx(final String peer, final long max, final long skipTo
              ) { return new Effect<Server>() {
        public void
        apply(final Database<Server> vat) throws Exception {
            final ImmutableArray<Effect<Server>> effects =
                vat.enter(Database.query,
                          new Transaction<ImmutableArray<Effect<Server>>>() {
                public ImmutableArray<Effect<Server>>
                apply(final Root root) throws Exception {
                    final Outbound outbound =
                        root.fetch(null, VatInitializer.outbound);
                    return outbound.find(peer).restart(max, skipTo);
                }
            }).call();
            for (final Effect<Server> effect : effects) {
                vat.service.apply(new Service() {
                    public Void
                    call() throws Exception {
                        effect.apply(vat);
                        return null;
                    }
                });
            }
        }
    }; }

    /**
     * Are there any pending messages in this pipeline?
     */
    protected boolean
    isPending() {
        if (!pending.isEmpty()) { return true; }
        for (final List<?> i : pollers) {
            if (!i.isEmpty()) { return true; }
        }
        return false;
    }

    /**
     * Restarts request sending.
     */
    protected ImmutableArray<Effect<Server>>
    resend() {
        ImmutableArray<Effect<Server>> r =
            restart(pending.getSize(), acknowledged);
        int priority = 0;
        for (final List<Operation> poller : pollers) {
            if (!poller.isEmpty()) {
                r = r.with(waitTx(peer, priority, 0L));
            }
            priority += 1;
        }
        return r;
    }

    /**
     * Enqueue an operation to retry until a non-404 response is received.
     * @param operation operation to enqueue
     * @return GUID assigned to request
     */
    protected Position
    poll(final Operation operation) { return enqueue(new Poller(operation)); }

    private final class
    Poller extends Operation implements Serializable {
        static private final long serialVersionUID = 1L;

        private final int a;                    // timeout before last request
        private final int b;                    // timeout before retry
        private final int priority;             // poller position for retry
        private final long attempts;            // number of 404 responses
        private final Operation underlying;     // request generator

        private
        Poller(final int a, final int b, final int priority,
               final long attempts, final Operation underlying) {
            super(false, false);
            this.a = a;
            this.b = b;
            this.priority = priority;
            this.attempts = attempts;
            this.underlying = underlying;
        }

        Poller(final Operation underlying) {
            this(0, 1, 0, 0L, underlying);
        }

        protected Message<Request>
        render(final String sessionKey,
               final long window, final int index) throws Exception {
            return underlying.render(sessionKey, window, index);
        }

        protected void
        fulfill(final Position position, final Message<Response> response) {
            underlying.fulfill(position, response);
            if (!"404".equals(response.head.status)) { return; }
            if (!isPending()) { Eventual.near(outbound).add(Pipeline.this); }
            final Poller retry = b > 3600 ?
                new Poller(3600, 3600,  priority,     attempts + 1, underlying):
                new Poller(b,    b + a, priority + 1, attempts + 1, underlying);
            final List<Operation> poller;
            if (priority < pollers.length()) {
                poller = pollers.get(priority);
            } else {
                poller = List.list();
                pollers = pollers.with(poller);
            }
            if (poller.isEmpty()) {
                effect.apply(waitTx(peer, priority, retry.a * 1000L));
            }
            poller.append(retry);
        }

        protected void
        reject(final Position position, final Exception reason) {
            underlying.reject(position, reason);
        }
    }

    static protected Effect<Server>
    waitTx(final String peer, final int priority, final long ms) {
        return new Effect<Server>() {
            public void
            apply(final Database<Server> vat) throws Exception {
                vat.scheduler.apply(ms, new Service() {
                    public Void
                    call() throws Exception {
                        vat.enter(Database.update, new Transaction<Immutable>(){
                            public Immutable
                            apply(final Root root) throws Exception {
                                final Outbound outbound =
                                    root.fetch(null, VatInitializer.outbound);
                                outbound.find(peer).tick(priority);
                                return new Token();
                            }
                        }).call();
                        return null;
                    }
                });
            }
        };
    }

    protected void
    tick(final int priority) {
        final List<Operation> poller = pollers.get(priority);
        if (0 == halts) {
            effect.apply(restartTx(peer, poller.getSize(),
                                   acknowledged + pending.getSize()));
        }
        while (!poller.isEmpty()) { pending.append(poller.pop()); }
    }

    /**
     * Enqueue an operation.
     * @param operation operation to enqueue
     * @return GUID assigned to request
     */
    protected Position
    enqueue(final Operation operation) {
        if (pending.isEmpty() && operation instanceof Task) {
            final Position position =
                new Position(0, 0, name + "-0-" + acknowledged);
            enqueue.apply(new ResolveTask((Task)operation, position));
            acknowledged += 1;
            return position;
        }
        if (!isPending()) { Eventual.near(outbound).add(this); }
        final long mid = acknowledged + pending.getSize();
        pending.append(operation);
        final Position r;
        if (operation.isUpdate) {
            if (0 != queries) {
                halts += 1;
                queries = 0;
                trailingIndex = -1;
            }
            trailingIndex += 1;
            r = new Position(activeWindow + halts, trailingIndex,
                             name+"-"+(activeWindow+halts)+"-"+trailingIndex);
        } else {
            r = new Position(0, 0, name + "-0-" + mid);
        }
        if (operation.isQuery) {
            queries += 1;
        }
        if (0 == halts && !(operation instanceof Task)) {
            effect.apply(restartTx(peer, 1, mid));
        }
        return r;
    }

    static protected final class
    Position implements Powerless, Selfless, Serializable {
        static private final long serialVersionUID = 1L;

        public final long window;
        public final int message;
        public final String guid;

        Position(final long window, final int message, final String guid) {
            this.window = window;
            this.message = message;
            this.guid = guid;
        }

        public boolean
        equals(final Object x) {
            return x instanceof Position &&
                   message == ((Position)x).message &&
                   window == ((Position)x).window &&
                   guid.equals(((Position)x).guid);
        }

        public int
        hashCode() { return guid.hashCode(); }

        protected boolean
        canPipeline() { return 0 != window; }
    }

    protected Operation
    dequeue(final long mid) {
        if (mid != acknowledged) { throw new RuntimeException(); }

        // clear all cached returns if processing a new messaging window
        if (-1 == priorIndex) {
          returns = ConstArray.array();
        }

        int completedQueries = 0;
        final Operation front = pending.pop();
        acknowledged += 1;
        if (front.isQuery) {
            completedQueries += 1;
        }
        while (!pending.isEmpty() && pending.getFront() instanceof Task) {
            final Task task = (Task)pending.pop();
            enqueue.apply(new ResolveTask(task,
                new Position(0, 0, name + "-0-" + acknowledged)));
            acknowledged += 1;
            if (task.isQuery) {
                completedQueries += 1;
            }
        }
        if (pending.isEmpty()) {
            activeWindow += 1;
            priorIndex = -1;
            halts = 0;
            queries = 0;
            trailingIndex = -1;
            if (!isPending()) { Eventual.near(outbound).remove(this); }
        } else {
            if (front.isUpdate) {
                priorIndex += 1;
            }
            if (0 != completedQueries) {
                if (0 == halts) {
                    queries -= completedQueries;
                } else {
                    /*
                     * restart message sending if this was the last query we
                     * were waiting on in the halted pipeline
                     */
                    long max = pending.getSize();
                    long skipTo = acknowledged;
                    for (final Operation x : pending) {
                        if (x.isUpdate) {
                            halts -= 1;
                            activeWindow += 1;
                            priorIndex = -1;
                            effect.apply(restartTx(peer, max, skipTo));
                            break;
                        }
                        if (x.isQuery) { break; }
                        --max;
                        ++skipTo;
                    }
                }
            }
        }
        return front;
    }

    static private Effect<Server>
    fulfill(final String peer, final Position position, final long mid,
            final Message<Request> q) { return new Effect<Server>() {
        public void
        apply(final Database<Server> vat) throws Exception {
            vat.session.serve(URI.scheme(peer), q.head,
                  null != q.body ? q.body.asInputStream() : null, new Client() {
                public void
                receive(final Response head,
                        final InputStream body) throws Exception {
                    Message<Response> r;
                    if (null != body) {
                        try {
                            final int length = head.getContentLength();
                            if (length > maxEntitySize) {
                                throw new TooBig(length, maxEntitySize);
                            }
                            r = new Message<Response>(head, Stream.snapshot(
                                    length >= 0 ? length : 1024,
                                    Limited.input(maxEntitySize + 1, body)));
                        } catch (final TooBig e) {
                            r = new Message<Response>(Response.tooBig(), null);
                        }
                    } else {
                        r = new Message<Response>(head, null);
                    }
                    final Message<Response> response = r;
                    vat.service.apply(new Service() {
                        public Void
                        call() throws Exception {
                            vat.enter(Database.update, fulfillTX(
                                peer, position, mid, response)).call();
                            return null;
                        }
                    });
                }
            });
        }
    }; }

    static protected Transaction<Immutable>
    fulfillTX(final String peer, final Position position, final long mid,
              final Message<Response> response) {
        return new Transaction<Immutable>() {
            public Immutable
            apply(final Root local) throws Exception {
                final Outbound outbound =
                    local.fetch(null, VatInitializer.outbound);
                outbound.find(peer).dequeue(mid).fulfill(position, response);
                return new Token();
            }
        };
    }

    static private Effect<Server>
    reject(final String peer, final Position position, final long mid,
           final Exception reason) { return new Effect<Server>() {
        public void
        apply(final Database<Server> vat) throws Exception {
            vat.session.serve(URI.scheme(peer), new Request("HTTP/1.1",
                    "HEAD", URI.request(peer), PowerlessArray.array(
                        new Header("Host",
                                   Authority.location(URI.authority(peer)))
                    )), null, new Client() {
                public void
                receive(final Response head,
                        final InputStream body) throws Exception {
                    vat.service.apply(new Service() {
                        public Void
                        call() throws Exception {
                            vat.enter(Database.update,
                                      rejectTX(peer, position, mid, reason));
                            return null;
                        }
                    });
                }
            });
        }
    }; }

    static protected Transaction<Immutable>
    rejectTX(final String peer, final Position position, final long mid,
             final Exception reason) { return new Transaction<Immutable>() {
        public Immutable
        apply(final Root local) throws Exception {
            final Outbound outbound = local.fetch(null,VatInitializer.outbound);
            outbound.find(peer).dequeue(mid).reject(position, reason);
            return new Token();
        }
    }; }
}
