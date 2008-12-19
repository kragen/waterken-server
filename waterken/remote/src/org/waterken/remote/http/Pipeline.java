// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.near;
import static org.web_send.Failure.maxEntitySize;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.ref_send.list.List;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.http.Client;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.Stream;
import org.waterken.io.limited.Limited;
import org.waterken.io.limited.TooBig;
import org.waterken.vat.Effect;
import org.waterken.vat.Root;
import org.waterken.vat.Service;
import org.waterken.vat.Transaction;
import org.waterken.vat.Vat;

/**
 * Manages a pending request queue for a specific host.
 */
final class
Pipeline implements Serializable {
    static private final long serialVersionUID = 1L;

    static private final class
    Entry extends Struct implements Serializable {
        static private final long serialVersionUID = 1L;

        final int id;           // serial number
        final Operation op;     // pending request
        
        Entry(final int id, final Operation op) {
            this.id = id;
            this.op = op;
        }
    }
    
    private final String peer;
    private final Receiver<Effect<Server>> effect;
    private final Fulfilled<Outbound> outbound;
    
    private final List<Entry> pending = List.list();
    private       int serialMID = 0;// serial number for next enqueued operation
    private       int halts = 0;    // number of pending pipeline flushes
    private       int queries = 0;  // number of queries after the last flush
    
    Pipeline(final String peer,
             final Receiver<Effect<Server>> effect,
             final Fulfilled<Outbound> outbound) {
        this.peer = peer;
        this.effect = effect;
        this.outbound = outbound;
    }

    void
    resend() { effect.run(restart(peer, pending.getSize(), false, 0)); }
    
    /*
     * Message sending is halted before an Update that follows a Query. Sending
     * resumes once the response to the Query has been received.
     */
    
    void
    enqueue(final Operation message) {
        if (pending.isEmpty()) {
            near(outbound).add(peer, this);
        }
        final int mid = serialMID++;
        pending.append(new Entry(mid, message));
        if (message instanceof Update) {
            if (0 != queries) {
                ++halts;
                queries = 0;
            }
        }
        if (message instanceof Query) { ++queries; }
        if (0 == halts) { effect.run(restart(peer, 1, true, mid)); }
    }
    
    private Operation
    dequeue(final int mid) {
        if (pending.getFront().id != mid) { throw new RuntimeException(); }
        
        final Entry front = pending.pop();
        if (pending.isEmpty()) {
            near(outbound).remove(peer);
        }
        if (front.op instanceof Query) {
            if (0 == halts) {
                --queries;
            } else {
                int max = pending.getSize();
                for (final Entry x : pending) {
                    if (x.op instanceof Update) {
                        --halts;
                        effect.run(restart(peer, max, true, x.id));
                        break;
                    }
                    if (x.op instanceof Query) { break; }
                    --max;
                }
            }
        }
        return front.op;
    }
    
    static private Effect<Server>
    restart(final String peer, final int max,
            final boolean skipTo, final int mid) {
        return new Effect<Server>() {
           public void
           run(final Vat<Server> vat) throws Exception {
               vat.enter(new Transaction<Immutable>(Transaction.query) {
                   public Immutable
                   run(final Root local) throws Exception {
                       final Receiver<Effect<Server>> effect =
                           local.fetch(null, Vat.effect);
                       final Outbound outbound =
                           local.fetch(null, VatInitializer.outbound);
                       boolean found = !skipTo;
                       boolean q = false;
                       int n = max;
                       for (final Entry x: outbound.find(peer).pending){
                           if (!found) {
                               if (mid == x.id) {
                                   found = true;
                               } else {
                                   continue;
                               }
                           }
                           if (0 == n--) { break; }
                           if (q && x.op instanceof Update) { break; }
                           if (x.op instanceof Query) { q = true; }
                           effect.run(send(peer, x.id, x.op));
                       }
                       return null;
                   }
               });
           }
        };
    }
    
    static private Effect<Server>
    send(final String peer, final int mid, final Operation op) {
        try {
            final Message<Request> m = op.render();
            return new Effect<Server>() {
                public void
                run(final Vat<Server> vat) throws Exception {
                    vat.session.serve(peer, m.head,
                        null != m.body ? m.body.asInputStream() : null,
                        new Fulfill(vat, peer, mid));
                }
            };
        } catch (final Exception reason) {
            return new Effect<Server>() {
                public void
                run(final Vat<Server> vat) throws Exception {
                    vat.session.serve(peer, null, null,
                                      new Reject(vat, peer, mid, reason));
                }
            };
        }
    }
    
    static private final class
    Reject extends Struct implements Client {
        
        private final Vat<Server> vat;
        private final String peer;
        private final int mid;
        private final Exception reason;
        
        protected
        Reject(final Vat<Server> vat, final String peer,
               final int mid, final Exception reason) {
            this.vat = vat;
            this.peer = peer;
            this.mid = mid;
            this.reason = reason;
        }
        
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
                            outbound.find(peer).dequeue(mid).reject(reason);
                            return null;
                        }
                    });
                    return null;
                }
            });
        }
    }
    
    static private final class
    Fulfill extends Struct implements Client {
        
        private final Vat<Server> vat;
        private final String peer;
        private final int mid;
        
        Fulfill(final Vat<Server> vat, final String peer, final int mid) {
            this.vat = vat;
            this.peer = peer;
            this.mid = mid;
        }

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
                            outbound.find(peer).dequeue(mid).fulfill(response);
                            return null;
                        }
                    });
                    return null;
                }
            });
        }
    }
}
