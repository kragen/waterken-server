// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.trace;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.uri.URI;

/**
 * A server that does a <code>TRACE</code> on a <code>GET</code> request.  
 */
public final class
Trace extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final Server next;

    /**
     * Constructs an instance.
     * @param next  next server to try
     */
    public @deserializer
    Trace(@name("next") final Server next) {
        this.next = next;
    }
    
    // org.waterken.http.Server interface

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
        request.expectContinue(respond);
    
        // We made it to the final processor, so bounce a TRACE.
        if ("TRACE".equals(request.method) || "GET".equals(request.method) ||
                                              "HEAD".equals(request.method)) {
            respond.fulfill(request.trace());
            return;
        }
    
        respond.fulfill(request.respond("TRACE, OPTIONS, GET, HEAD"));
    }
}
