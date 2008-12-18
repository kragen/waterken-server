// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.web_send.Failure.maxEntitySize;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.charset.URLEncoding;
import org.ref_send.deserializer;
import org.ref_send.promise.Promise;
import org.waterken.http.Client;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.Stream;
import org.waterken.io.limited.Limited;
import org.waterken.remote.mux.Remoting;
import org.waterken.uri.Path;
import org.waterken.uri.URI;
import org.waterken.vat.Root;
import org.waterken.vat.Transaction;
import org.waterken.vat.Vat;
import org.web_send.Failure;

/**
 * web-key implementation
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
    remote(final Server bootstrap, final String scheme, final Vat<Server> vat) {
        return new Server() {
            public void
            serve(final String resource, final Request head,
                  final InputStream body, final Client client) throws Exception{

                // check for web browser bootstrap request
                final String q = URI.query(null, resource);
                if (null == q) {
                    bootstrap.serve("file:///site/" +
                            URLEncoding.encode(vat.getProject()) + "/" +
                            URLEncoding.encode(Path.name(URI.path(resource))),
                        head, body, client);
                    return;
                }

                final int length = head.getContentLength();
                if (length > maxEntitySize) { throw Failure.tooBig(); }
                if (!head.expect(client,"GET","HEAD","POST","OPTIONS","TRACE")){
                    return;
                }
                final ByteArray buffered = null == body
                    ? null
                : Stream.snapshot(length >= 0 ? length : 1024,
                                  Limited.input(maxEntitySize, body));
                final Promise<Message<Response>> respondor =
                        vat.enter(new Transaction<Message<Response>>(
                                      "GET".equals(head.method) ||
                                      "HEAD".equals(head.method) ||
                                      "OPTIONS".equals(head.method) ||
                                      "TRACE".equals(head.method)) {
                    public Message<Response>
                    run(final Root local) throws Exception {
                        final ClassLoader code = local.fetch(null, Vat.code);
                        final Exports exports = new Exports(local); 
                        return new Callee(code, exports).run(q, head, buffered);
                    }
                });
                final Message<Response> r;
                try {
                    r = respondor.cast();
                } catch (final Exception e) {
                    client.failed(e);
                    return;
                }
                client.receive(r.head,null!=r.body?r.body.asInputStream():null);
            }
        };
    }
}
