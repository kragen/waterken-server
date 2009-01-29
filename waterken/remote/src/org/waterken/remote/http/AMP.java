// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.eventual.Failure.maxEntitySize;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.joe_e.charset.URLEncoding;
import org.ref_send.deserializer;
import org.ref_send.promise.eventual.Receiver;
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
                bootstrap.serve(new Request(head.version, head.method,
                        "/site/" + URLEncoding.encode(vat.getProject()) + "/" +
                        URLEncoding.encode(Path.name(URI.path(head.uri))),
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
            vat.service.run(new Service() {
                public Void
                run() throws Exception {
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
                            run(final Root local) throws Exception {
                                final HTTP.Exports exports =
                                    local.fetch(null, VatInitializer.exports);
                                return new Callee(exports).run(q, m);
                            }
                        }).cast();
                    } catch (final FileNotFoundException e) {
                        client.receive(Response.gone(), null);
                        throw e;
                    } catch (final Exception e) {
                        client.fail(e);
                        throw e;
                    }
                    client.receive(r.head,
                                   null!=r.body ?r.body.asInputStream() :null);
                    return null;
                }
            });
        }
    }; }

    static private <T> Receiver<T>
    poster(final String href, final Server proxy) { return new Receiver<T>() {
        public void
        run(final Object value) {
            try {
                final String target = HTTP.post(href, "run", null, 0, 0);
                final Message<Request> q =
                    Caller.serialize("", null, target, ConstArray.array(value));
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
        run(final String href, final String base, final Type type) {
            return poster(null != base ? URI.resolve(base, href) : href, proxy);
        }
    }; }
}
