// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.trace;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * A server that does a <code>TRACE</code> on a <code>GET</code> request.  
 */
public final class
Trace {

    private
    Trace() {}
    
    /**
     * Constructs an instance.
     * @param next  next server to try
     */
    static public Server
    make(final Server next) {
        class ServerX extends Struct implements Server, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            serve(final String resource,
                  final Volatile<Request> requestor,
                  final Do<Response,?> respond) throws Exception {
                
                if (!URI.path(resource).startsWith("trace/")) {
                    next.serve(resource, requestor, respond);
                    return;
                }

                // Determine the request.
                final Request request;
                try {
                    request = requestor.cast();
                } catch (final Exception e) {
                    respond.reject(e);
                    return;
                }

                // We made it to the final processor, so bounce a TRACE.
                if ("TRACE".equals(request.method) ||
                        "GET".equals(request.method) ||
                        "HEAD".equals(request.method)) {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "200", "OK",
                        PowerlessArray.array(
                            new Header("Content-Type",
                                       "message/http; charset=iso-8859-1")
                        ), "HEAD".equals(request.method) ? null : request));
                    return;
                }

                if ("OPTIONS".equals(request.method)) {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "204", "OK",
                        PowerlessArray.array(
                            new Header("Allow", "TRACE, OPTIONS, GET, HEAD")
                        ), null));
                    return;
                }
                respond.fulfill(new Response(
                    "HTTP/1.1", "405", "Method Not Allowed",
                    PowerlessArray.array(
                        new Header("Allow", "TRACE, OPTIONS, GET, HEAD"),
                        new Header("Content-Length", "0")
                    ), null));
            }
        }
        return new ServerX();
    }
}
