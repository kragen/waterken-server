// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Failure.maxEntitySize;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Immutable;
import org.joe_e.Token;
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

    protected
    Pipeline(final String peer, final String key, final String name,
             final Receiver<Effect<Server>> effect,
             final Promise<Outbound> outbound) {
        this.peer = peer;
        this.key = key;
        this.name = name;
        this.effect = effect;
        this.outbound = outbound;
    }

    protected void
    resend() { effect.apply(restart(peer, pending.getSize(), acknowledged + 1)); }
    
    /**
     * Enqueue an operation.
     * @param operation operation to enqueue
     * @return GUID assigned to request
     */
    protected String
    enqueue(final Operation operation) {
        if (pending.isEmpty()) {
            Eventual.near(outbound).add(this);
        }
        pending.append(operation);
        final long mid = acknowledged + pending.getSize();
        final String guid;
        if (operation instanceof UpdateOperation) {
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
        if (operation instanceof QueryOperation) {
            queries += 1;
        }
        if (0 == halts) { effect.apply(restart(peer, 1, mid)); }
        return guid;
    }
    
    private Operation
    dequeue(final long mid) {
        if (mid != acknowledged + 1) { throw new RuntimeException(); }
        
        final Operation front = pending.pop();
        if (pending.isEmpty()) {
            Eventual.near(outbound).remove(this);
        }
        acknowledged += 1;
        if (front instanceof UpdateOperation) {
            activeIndex += 1;
        }
        if (front instanceof QueryOperation) {
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
                    if (x instanceof UpdateOperation) {
                        halts -= 1;
                        activeWindow += 1;
                        activeIndex = -1;
                        effect.apply(restart(peer, max, skipTo));
                        break;
                    }
                    if (x instanceof QueryOperation) { break; }
                    --max;
                }
            }
        }
        return front;
    }
    
    /**
     * Serializes requests and enqueues them on the transient HTTP connection.
     * @param peer      absolute URI for the pipeline to service
     * @param max       maximum number of requests to enqueue
     * @param skipTo    id of first request to send
     */
    static private Effect<Server>
    restart(final String peer, final int max, final long skipTo
           ) { return new Effect<Server>() {
        public void
        apply(final Database<Server> vat) throws Exception {
            vat.enter(Transaction.query, new Transaction<Immutable>() {
                public Immutable
                apply(final Root root) throws Exception {
                    final Receiver<Effect<Server>> effect =
                        root.fetch(null, Database.effect);
                    final Outbound outbound =
                        root.fetch(null, VatInitializer.outbound);
                    final Pipeline m = outbound.find(peer);
                    final long window = m.activeWindow;
                    int index = m.activeIndex;
                    boolean queried = false;
                    long sent = m.acknowledged;
                    boolean found = false;
                    int n = max;
                    for (final Operation x : m.pending) {
                        if (x instanceof UpdateOperation) {
                            if (queried) { break; }
                            index += 1;
                        }
                        if (x instanceof QueryOperation) { queried = true; }
                        sent += 1;
                        found = found || skipTo == sent;
                        if (!found) { continue; }
                        if (0 == n--) { break; }
                       
                        final long mid = sent;
                        final String guid;
                        if (x instanceof UpdateOperation) {
                            guid = m.name + "-" + window + "-" + index;
                        } else {
                            guid = m.name + "-0-" + mid;
                        }
                        try {
                            final Message<Request> q =
                                x.render(m.key, window, index);
                            effect.apply(new Effect<Server>() {
                                public void
                                apply(Database<Server> vat) throws Exception {
                                    vat.session.serve(q.head,
                                      null!=q.body?q.body.asInputStream():null,
                                      fulfill(vat, peer, guid, mid));
                                }
                            });
                        } catch (final Exception reason) {
                            final String authority = URI.authority(peer);
                            final String location=Authority.location(authority);
                            effect.apply(new Effect<Server>() {
                                public void
                                apply(Database<Server> vat) throws Exception {
                                    vat.session.serve(
                                        new Request("HTTP/1.1", "OPTIONS",
                                            URI.request(peer),
                                            PowerlessArray.array(
                                                new Header("Host", location)
                                            )), null,
                                        reject(vat, peer, guid, mid, reason));
                                }
                            });
                        }
                    }
                    return new Token();
                }
            }).call();
        }
    }; }
    
    static private Client
    reject(final Database<Server> vat, final String peer,
           final String request, final long mid, final Exception reason
          ) { return new Client() {
        public void
        receive(final Response head, final InputStream body) throws Exception {
            vat.service.apply(new Service() {
                public Void
                call() throws Exception {
                    vat.enter(Transaction.update, new Transaction<Immutable>() {
                        public Immutable
                        apply(final Root local) throws Exception {
                            final Outbound outbound =
                                local.fetch(null, VatInitializer.outbound);
                            outbound.find(peer).dequeue(mid).
                                reject(request, reason);
                            return new Token();
                        }
                    }).call();
                    return null;
                }
            });
        }
    }; }
    
    static private Client
    fulfill(final Database<Server> vat, final String peer, 
            final String request, final long mid) { return new Client() {
        public void
        receive(final Response head, final InputStream body) throws Exception {
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
                    vat.enter(Transaction.update, new Transaction<Immutable>() {
                        public Immutable
                        apply(final Root local) throws Exception {
                            final Outbound outbound =
                                local.fetch(null, VatInitializer.outbound);
                            outbound.find(peer).dequeue(mid).
                                fulfill(request, response);
                            return new Token();
                        }
                    }).call();
                    return null;
                }
            });
        }
    }; }
}
