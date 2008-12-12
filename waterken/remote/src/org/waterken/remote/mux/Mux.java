// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.mux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Server;
import org.waterken.uri.Path;
import org.waterken.uri.URI;
import org.waterken.vat.VatManager;
import org.web_send.Failure;

/**
 * Puts the persistent databases into the URI hierarchy.
 */
public final class
Mux<S> extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final String vatURIPathPrefix;
    private final File vatRoot;
    private final VatManager<S> vats;
    private final Remoting<S> remoting;
    private final Server next;
    
    /**
     * Constructs an instance.
     * @param vatURIPathPrefix  URI sub-hierarchy for persistent databases
     * @param vatRoot           root persistence folder
     * @param vats              open vat pool
     * @param remoting          remoting protocol
     * @param next              default server
     */
    public @deserializer
    Mux(@name("vatURIPathPrefix") final String vatURIPathPrefix,
        @name("vatRoot") final File vatRoot,
        @name("vats") final VatManager<S> vats,
        @name("remoting") final Remoting<S> remoting,
        @name("next") final Server next) {
        this.vatURIPathPrefix = vatURIPathPrefix;
        this.vatRoot = vatRoot;
        this.vats = vats;
        this.remoting = remoting;
        this.next = next;
    }

    // org.waterken.http.Server interface

    public void
    serve(final String resource, final Request head, final InputStream body,
                                 final Client client) throws Exception {
        final Server server;
        final String path = URI.path(resource);
        if (path.startsWith(vatURIPathPrefix)) {
            final String vatPath = path.substring(vatURIPathPrefix.length());
            try {
                final File folder = Path.descend(vatRoot, vatPath);
                server = remoting.remote(next, URI.scheme(null, resource),
                                         vats.connect(folder));
            } catch (final InvalidFilenameException e) {
                client.failed(Failure.gone());
                return;
            } catch (final FileNotFoundException e) {
                client.failed(Failure.gone());
                return;
            }
        } else {
            server = next;
        }
        server.serve(resource, head, body, client);
    }
}
