// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.near;
import static org.web_send.Failure.maxEntitySize;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Immutable;
import org.ref_send.list.List;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.Service;
import org.waterken.db.Transaction;
import org.waterken.db.Vat;
import org.waterken.http.Client;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.Stream;
import org.waterken.io.limited.Limited;
import org.waterken.io.limited.TooBig;

/**
 * Manages a pending request queue for a peer vat.
 */
/* package */ final class
Pipeline implements Serializable {
    static private final long serialVersionUID = 1L;
    
    private   final String name;                    // messaging session name
    protected final String peer;                    // URI of peer vat
    private   final Receiver<Effect<Server>> effect;
    private   final Fulfilled<Outbound> outbound;
   
    private         long activeWindow = 1;  // id of on-the-air window
    private         int  activeIndex = -1;  // last acknowledged window index
    private         long acknowledged = 0;  // total acknowledged operations 
    
    private   final List<Operation> pending = List.list();
    private         int halts = 0;      // number of pending pipeline flushes
    private         int queries = 0;    // number of queries after last flush
    private         int updates = 0;    // number of updates after last flush
    
    /*
     * Message sending is halted before an Update that follows a Query. Sending
     * resumes once the response to the Query has been received.
     */
    
    private         String key = "";    // messaging session key

    protected
    Pipeline(final String name, final String peer,
             final Receiver<Effect<Server>> effect,
             final Fulfilled<Outbound> outbound) {
        this.name = name;
        this.peer = peer;
        this.effect = effect;
        this.outbound = outbound;
    }

    protected void
    resend() { effect.run(restart(peer, pending.getSize(), acknowledged + 1)); }
    
    /**
     * Enqueue an operation.
     * @param operation operation to enqueue
     * @return GUID assigned to request
     */
    protected String
    enqueue(final Operation operation) {
        if (pending.isEmpty()) {
            near(outbound).add(this);
        }
        pending.append(operation);
        final long mid = acknowledged + pending.getSize();
        final String guid;
        if (operation instanceof Update) {
            // TODO: initialize session key
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
        if (operation instanceof Query) {
            queries += 1;
        }
        if (0 == halts) { effect.run(restart(peer, 1, mid)); }
        return guid;
    }
    
    private Operation
    dequeue(final long mid) {
        if (mid != acknowledged + 1) { throw new RuntimeException(); }
        
        final Operation front = pending.pop();
        if (pending.isEmpty()) {
            near(outbound).remove(peer);
        }
        acknowledged += 1;
        if (front instanceof Update) {
            activeIndex += 1;
        }
        if (front instanceof Query) {
            if (0 == halts) {
                queries -= 1;
            } else {
                /*
                 * restart message sending if this was the last query we were
                 * waiting on in the halted pipeline
                 */
                int max = pending.getSize();
                long skipTo = acknowledged;
                for (final Operation x : pending) {
                    ++skipTo;
                    if (x instanceof Update) {
                        halts -= 1;
                        activeWindow += 1;
                        activeIndex = -1;
                        effect.run(restart(peer, max, skipTo));
                        break;
                    }
                    if (x instanceof Query) { break; }
                    --max;
                }
            }
        }
        return front;
    }
    
    /**
     * Serializes requests and enqueues them on the transient HTTP connection.
     * @param peer      id for the pipeline to service
     * @param max       maximum number of requests to enqueue
     * @param skipTo    id of first request to send
     */
    static private Effect<Server>
    restart(final String peer, final int max, final long skipTo
           ) { return new Effect<Server>() {
        public void
        run(final Vat<Server> vat) throws Exception {
            vat.enter(new Transaction<Immutable>(Transaction.query) {
                public Immutable
                run(final Root local) throws Exception {
                    final Receiver<Effect<Server>> effect =
                        local.fetch(null, Vat.effect);
                    final Outbound outbound =
                        local.fetch(null, VatInitializer.outbound);
                    final Pipeline m = outbound.find(peer);
                    final long window = m.activeWindow;
                    int index = m.activeIndex;
                    boolean queried = false;
                    long sent = m.acknowledged;
                    boolean found = false;
                    int n = max;
                    for (final Operation x : m.pending) {
                        if (x instanceof Update) {
                            if (queried) { break; }
                            index += 1;
                        }
                        if (x instanceof Query) { queried = true; }
                        sent += 1;
                        found = found || skipTo == sent;
                        if (!found) { continue; }
                        if (0 == n--) { break; }
                       
                        final long mid = sent;
                        final String guid;
                        if (x instanceof Update) {
                            guid = m.name + "-" + window + "-" + index;
                        } else {
                            guid = m.name + "!" + mid;
                        }
                        try {
                            final Message<Request> q =
                                x.render(m.key, window, index);
                            effect.run(new Effect<Server>() {
                                public void
                                run(final Vat<Server> vat) throws Exception {
                                    vat.session.serve(peer, q.head,
                                      null!=q.body?q.body.asInputStream():null,
                                      fulfill(vat, peer, guid, mid));
                                }
                            });
                        } catch (final Exception reason) {
                            effect.run(new Effect<Server>() {
                                public void
                                run(final Vat<Server> vat) throws Exception {
                                    vat.session.serve(peer, null, null,
                                        reject(vat, peer, guid, mid, reason));
                                }
                            });
                        }
                    }
                    return null;
                }
            });
        }
    }; }
    
    static private Client
    reject(final Vat<Server> vat, final String peer,
           final String request, final long mid, final Exception reason
          ) { return new Client() {
        public void
        run(final Response head, final InputStream body) throws Exception {
            vat.service.run(new Service() {
                public Void
                run() throws Exception {
                    vat.enter(new Transaction<Immutable>(Transaction.update) {
                        public Immutable
                        run(final Root local) throws Exception {
                            final Outbound outbound =
                                local.fetch(null, VatInitializer.outbound);
                            outbound.find(peer).dequeue(mid).
                                reject(request, reason);
                            return null;
                        }
                    });
                    return null;
                }
            });
        }
    }; }
    
    static private Client
    fulfill(final Vat<Server> vat, final String peer, 
            final String request, final long mid) { return new Client() {
        public void
        run(final Response head, final InputStream body) throws Exception {
            Message<Response> r;
            if (null != body) {
                try {
                    final int length = head.getContentLength();
                    if (length > maxEntitySize) { throw new TooBig(); }
                    r = new Message<Response>(head,
                            Stream.snapshot(length >= 0 ? length : 1024,
                                            Limited.input(maxEntitySize,body)));
                } catch (final TooBig e) {
                    r = new Message<Response>(Response.tooBig(), null);
                }
            } else {
                r = new Message<Response>(head, null);
            }
            final Message<Response> response = r;
            vat.service.run(new Service() {
                public Void
                run() throws Exception {
                    vat.enter(new Transaction<Immutable>(Transaction.update) {
                        public Immutable
                        run(final Root local) throws Exception {
                            final Outbound outbound =
                                local.fetch(null, VatInitializer.outbound);
                            outbound.find(peer).dequeue(mid).
                                fulfill(request, response);
                            return null;
                        }
                    });
                    return null;
                }
            });
        }
    }; }
}
