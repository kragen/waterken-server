// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.mux;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.Filesystem;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.jos.JODB;
import org.waterken.remote.Remoting;
import org.waterken.uri.Path;
import org.waterken.uri.URI;
import org.web_send.Failure;

/**
 * Puts the persistent databases into the URI hierarchy.
 */
public final class
Mux extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final String vatURIPathPrefix;
    private final File vatRootFolder;
    private final Remoting remoting;
    private final Server next;
    
    /**
     * Constructs an instance.
     * @param vatURIPathPrefix  URI sub-hierarchy for persistent databases
     * @param vatRootFolder     root persistence folder
     * @param remoting          remoting protocol
     * @param next              default server
     */
    public @deserializer
    Mux(@name("vatURIPathPrefix") final String vatURIPathPrefix,
        @name("vatRootFolder") final File vatRootFolder,
        @name("remoting") final Remoting remoting,
        @name("next") final Server next) {
        this.vatURIPathPrefix = vatURIPathPrefix;
        this.vatRootFolder = vatRootFolder;
        this.remoting = remoting;
        this.next = next;
    }

    // org.waterken.http.Server interface

    public void
    serve(final String resource, final Volatile<Request> request,
          final Do<Response,?> respond) throws Exception {
        final Server server;
        final String path = URI.path(resource);
        if (path.startsWith(vatURIPathPrefix)) {
            final String vatPath = path.substring(vatURIPathPrefix.length());
            File folder = vatRootFolder;
            for (final String name : Path.walk(vatPath)) {
                if (name.startsWith(".")) {
                    respond.reject(Failure.gone());
                    return;
                }
                folder = Filesystem.file(folder, name);
            }
            try {
                server = remoting.remote(next,
                    URI.scheme(null, resource), JODB.connect(folder));
            } catch (final FileNotFoundException e) {
                respond.reject(Failure.gone());
                return;
            }
        } else {
            server = next;
        }
        server.serve(resource, request, respond);
    }
}
