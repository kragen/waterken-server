// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.servlets;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;

import org.joe_e.Struct;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.scope.Scope;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.uri.Path;
import org.waterken.uri.URI;

/**
 * Puts servlets into the URI hierarchy.
 */
public final class
Servlets extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final String prefix;
    private final Scope<?> servlets;
    private final Server next;

    /**
     * Constructs an instance.
     * @param prefix    URI sub-hierarchy for servlets
     * @param servlets  name to implementation map    
     * @param next      default server
     */
    public @deserializer
    Servlets(@name("prefix") final String prefix,
             @name("servlets") final Scope<?> servlets,
             @name("next") final Server next) {
        this.prefix = prefix;
        this.servlets = servlets;
        this.next = next;
    }

    // org.waterken.http.Server interface

    public void
    serve(final String scheme, final Request head,
          final InputStream body, final Client client) throws Exception {
        final Server server;
        final String path = URI.path(head.uri);
        if (path.startsWith(prefix)) {
            final Iterator<String> segments =
                Path.walk(path.substring(prefix.length())).iterator();
            if (segments.hasNext()) {
                server = servlets.get(segments.next());
            } else {
                server = null;
            }
        } else {
            server = next;
        }
        if (null == server) {
            client.receive(Response.gone(), null);
        } else {
            server.serve(scheme, head, body, client);
        }
    }
}
