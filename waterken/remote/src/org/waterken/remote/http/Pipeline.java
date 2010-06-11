// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Failure.maxEntitySize;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Immutable;
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
Pipeline implements Serializable {
    static private final long serialVersionUID = 1L;
    
    protected final String peer;                    // absolute URI of peer vat
    private   final String key;                     // messaging session key
    private   final String name;                    // messaging session name
    private   final Receiver<Effect<Server>> effect;
    private   final Promise<Outbound> outbound;
    
    private   final ConstArray<List<Operation>> pollers;
   
    private         long activeWindow = 1;  // id of on-the-air window
    private         int  activeIndex = -1;  // last acknowledged window index
    private         long acknowledged = 0;  // total acknowledged operations 
    
    private   final List<Operation> pending = List.list();
    private         long halts = 0;     // number of pending pipeline flushes
    private         long queries = 0;   // number of queries after last flush
    private         int updates = 0;    // number of updates after last flush
    
    /*
     * Message sending is halted before an Update that follows a Query. Sending
     * resumes once the response to the Query has been received.
     */

    protected
    Pipeline(final String peer, final String key, final String name,
             final Receiver<Effect<Server>> effect,
             final Promise<Outbound> outbound) {
        this.peer = peer;
        this.key = key;
        this.name = name;
        this.effect = effect;
        this.outbound = outbound;
        
        final ConstArray.Builder<List<Operation>> b = ConstArray.builder(5);
        for (int i = 5; 0 != i--;) {
            final List<Operation> l = List.list();
            b.append(l);
        }
        pollers = b.snapshot();
    }
    
    /**
     * Serializes requests and enqueues them on the transient HTTP connection.
     * @param max       maximum number of requests to enqueue
     * @param skipTo    mid of first request to send
     */
    private ImmutableArray<Effect<Server>>
    restart(final long max, final long skipTo) {
        final ImmutableArray.Builder<Effect<Server>> r=ImmutableArray.builder();
        int index = activeIndex;
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
           
            final String guid;
            if (x.isUpdate) {
                guid = name + "-" + activeWindow + "-" + index;
            } else {
                guid = name + "-0-" + mid;
            }
            try {
                r.append(fulfill(peer, guid, mid,
                                 x.render(key, activeWindow, index)));
            } catch (final Exception reason) {
                r.append(reject(peer, guid, mid, reason));
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
                    @Override public Void
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
    private boolean
    isActive() {
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
    protected String
    poll(final Operation operation) {return enqueue(new Poller(0, operation));}
    
    private final class
    Poller extends Operation implements Serializable {
        static private final long serialVersionUID = 1L;

        private final long retries;
        private final Operation underlying;
        
        Poller(final long retries, final Operation underlying) {
            super(false, false);
            this.retries = retries;
            this.underlying = underlying;
        }
        
        protected Message<Request>
        render(final String sessionKey,
               final long window, final int index) throws Exception {
            return underlying.render(sessionKey, window, index);
        }

        protected void
        fulfill(final String request, final Message<Response> response) {
            underlying.fulfill(request, response);
            if ("404".equals(response.head.status)) {
                if (!isActive()) { Eventual.near(outbound).add(Pipeline.this); }
                final int priority = (int)Math.min(pollers.length()-1, retries); 
                final List<Operation> poller = pollers.get(priority);
                if (poller.isEmpty()) {
                    effect.apply(waitTx(peer, priority, 1000L << 3 * priority));
                }
                poller.append(new Poller(retries + 1, underlying));
            }
        }

        protected void
        reject(final String request, final Exception reason) {
            underlying.reject(request, reason);
        }
    }
    
    static private Effect<Server>
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
    
    private void
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
    protected String
    enqueue(final Operation operation) {
        if (!isActive()) { Eventual.near(outbound).add(this); }
        final long mid = acknowledged + pending.getSize();
        pending.append(operation);
        final String guid;
        if (operation.isUpdate) {
            if (0 != queries) {
                halts += 1;
                queries = 0;
                updates = 0;
            }
            guid = name + "-" + (activeWindow + halts) + "-" + updates;
            updates += 1;
        } else {
            guid = name + "-0-" + mid;
        }
        if (operation.isQuery) {
            queries += 1;
        }
        if (0 == halts) { effect.apply(restartTx(peer, 1, mid)); }
        return guid;
    }
    
    private Operation
    dequeue(final long mid) {
        if (mid != acknowledged) { throw new RuntimeException(); }
        
        final Operation front = pending.pop();
        acknowledged += 1;
        if (pending.isEmpty()) {
            activeWindow += 1;
            activeIndex = -1;
            halts = 0;
            queries = 0;
            updates = 0;
            if (!isActive()) { Eventual.near(outbound).remove(this); }
        } else {
            if (front.isUpdate) {
                activeIndex += 1;
            }
            if (front.isQuery) {
                if (0 == halts) {
                    queries -= 1;
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
                            activeIndex = -1;
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
    fulfill(final String peer, final String request, final long mid,
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
                            if (length > maxEntitySize) { throw new TooBig(); }
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
                            vat.enter(Database.update,
                                fulfillTX(peer, request, mid, response)).call();
                            return null;
                        }
                    });
                }
            });
        }
    }; }
    
    static private Transaction<Immutable>
    fulfillTX(final String peer, final String request, final long mid,
              final Message<Response> response) {
        return new Transaction<Immutable>() {
            public Immutable
            apply(final Root local) throws Exception {
                final Outbound outbound =
                    local.fetch(null, VatInitializer.outbound);
                outbound.find(peer).dequeue(mid).fulfill(request, response);
                return new Token();
            }
        };
    }
    
    static private Effect<Server>
    reject(final String peer, final String request, final long mid,
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
                                      rejectTX(peer, request, mid, reason));
                            return null;
                        }
                    });
                }
            });
        }
    }; }
    
    static private Transaction<Immutable>
    rejectTX(final String peer, final String request, final long mid,
             final Exception reason) { return new Transaction<Immutable>() {
        public Immutable
        apply(final Root local) throws Exception {
            final Outbound outbound = local.fetch(null,VatInitializer.outbound);
            outbound.find(peer).dequeue(mid).reject(request, reason);
            return new Token();
        }
    }; }
}
