// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Failure.maxEntitySize;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.URLEncoding;
import org.ref_send.deserializer;
import org.ref_send.promise.Receiver;
import org.waterken.db.Database;
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
import org.waterken.remote.mux.Remoting;
import org.waterken.store.DoesNotExist;
import org.waterken.syntax.Importer;
import org.waterken.uri.Path;
import org.waterken.uri.URI;

/**
 * Factory for the server-side of the web-key protocol.
 */
public final class
AMP extends Struct implements Remoting<Server>, Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs an instance.
     */
    public @deserializer
    AMP() {}
    
    // org.waterken.remote.mux.Remoting interface

    public Server
    remote(final Server bootstrap,
           final Database<Server> vat) { return new Server() {
        public void
        serve(final Request head,
              final InputStream body, final Client client) throws Exception {

            // check for web browser bootstrap request
            final String q = URI.query(null, head.uri);
            if (null == q) {
                final String project;
                try {
                    project = vat.enter(Transaction.query,
                            new Transaction<PowerlessArray<String>>() {
                        public PowerlessArray<String>
                        apply(final Root root) throws Exception {
                            final String r = root.fetch(null, Database.project);
                            return PowerlessArray.array(r);
                        }
                    }).call().get(0);
                } catch (final DoesNotExist e) {
                    client.receive(Response.gone(), null);
                    return;
                }
                bootstrap.serve(new Request(head.version, head.method,
                        "/site/" + URLEncoding.encode(project) + "/" +
                        URLEncoding.encode(Path.name(URI.path(head.uri)))+"?o=",
                        head.headers), body, client);
                return;
            }

            final int length = head.getContentLength();
            if (length > maxEntitySize) { throw new TooBig(); }
            if (!head.expect(client, "TRACE","OPTIONS","GET","HEAD","POST")) {
                return;
            }
            final Message<Request> m = new Message<Request>(head, null == body
                ? null : Stream.snapshot(length >= 0 ? length : 512,
                                         Limited.input(maxEntitySize, body)));
            vat.service.apply(new Service() {
                public Void
                call() throws Exception {
                    if (!client.isStillWaiting()) {
                        client.fail(new Exception());
                        return null;
                    }
                    
                    final Message<Response> r;
                    try {
                        r = vat.enter("GET".equals(head.method) ||
                                      "HEAD".equals(head.method) ||
                                      "OPTIONS".equals(head.method) ||
                                      "TRACE".equals(head.method),
                                      new Transaction<Message<Response>>() {
                            public Message<Response>
                            apply(final Root root) throws Exception {
                                final HTTP.Exports exports =
                                    root.fetch(null, VatInitializer.exports);
                                return new Callee(exports).apply(q, m);
                            }
                        }).call();
                    } catch (final DoesNotExist e) {
                        client.receive(Response.gone(), null);
                        return null;
                    } catch (final Exception e) {
                        client.fail(e);
                        throw e;
                    }
                    try {
                        client.receive(
                            r.head, null!=r.body ?r.body.asInputStream() :null);
                    } catch (final IOException e) {}
                    return null;
                }
            });
        }
    }; }

    static private <T> Receiver<T>
    poster(final String href, final Server proxy) { return new Receiver<T>() {
        public void
        apply(final Object value) {
            try {
                final String target = HTTP.post(href, "apply", null, 0, 0);
                final Message<Request> q = Caller.serialize("", null, target,
                     ConstArray.array((Type)Object.class),
                     ConstArray.array(value));
                proxy.serve(q.head, q.body.asInputStream(), new Client() {
                    public void
                    receive(final Response head, final InputStream body) {}
                });
            } catch (final Exception e) {}
        }
    }; }
    
    static public Importer
    connect(final Server proxy) { return new Importer() {
        public Object
        apply(final String href, final String base, final Type type) {
            return poster(null != base ? URI.resolve(base, href) : href, proxy);
        }
    }; }
}
