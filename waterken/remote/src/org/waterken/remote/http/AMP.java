// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.eventual.Failure.maxEntitySize;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.charset.URLEncoding;
import org.ref_send.deserializer;
import org.waterken.db.Database;
import org.waterken.db.Root;
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
            if (length > maxEntitySize) {
                client.run(Response.tooBig(), null);
                throw new TooBig();
            }
            if (!head.expect(client, "TRACE","OPTIONS","GET","HEAD","POST")) {
                return;
            }
            final Message<Request> m = new Message<Request>(head, null == body
                ? null : Stream.snapshot(length >= 0 ? length : 1024,
                                         Limited.input(maxEntitySize, body)));
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
                client.run(Response.gone(), null);
                return;
            } 
            client.run(r.head, null != r.body ? r.body.asInputStream() : null);
        }
    }; }
}
