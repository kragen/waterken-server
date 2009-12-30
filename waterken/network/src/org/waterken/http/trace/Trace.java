// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.trace;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.deserializer;
import org.waterken.http.Client;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;

/**
 * A server that does a <code>TRACE</code> on a <code>GET</code> request.  
 */
public final class
Trace extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    Trace() {}
    
    // org.waterken.http.Server interface

    public void
    serve(final String scheme, final Request head,
          final InputStream body, final Client client) throws Exception {        

        // obey any request restrictions
        if (!head.respond(null,client,"TRACE","OPTIONS","GET","HEAD")) {return;}
        
        final Message<Response> r = head.trace();
        client.receive(r.head, r.body.asInputStream());
    }
}
