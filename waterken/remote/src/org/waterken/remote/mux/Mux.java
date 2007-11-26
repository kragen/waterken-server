// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.mux;

import java.io.File;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.Filesystem;
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
Mux {
    
    /**
     * URI sub-hierarchy for persistent databases
     */
    static public final String dbPathPrefix = "-/";
    
    /**
     * Constructs an instance.
     * @param db        persistence directory
     * @param remoting  remoting protocol
     * @param next      default server
     */
    static public Server
    make(final File db, final Remoting remoting, final Server next) {
        class ServerX extends Struct implements Server, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            serve(final String resource,
                  final Volatile<Request> request,
                  final Do<Response,?> respond) throws Exception {
                final Server server;
                final String path = URI.path(resource);
                if (path.startsWith(dbPathPrefix)) {
                    final String dbPath = path.substring(dbPathPrefix.length());
                    File folder = db;
                    for (final String name : Path.walk(dbPath)) {
                        if (name.startsWith(".")) {
                            respond.reject(Failure.gone());
                            return;
                        }
                        folder = Filesystem.file(folder, name);
                    }

                    // check that folder still exists
                    if (!folder.isDirectory()) {
                        respond.reject(Failure.gone());
                        return;
                    }
                    server = remoting.remote(next, URI.scheme(null, resource),
                                             JODB.connect(folder));
                } else {
                    server = next;
                }
                server.serve(resource, request, respond);
            }
        }
        return new ServerX();
    }
}
