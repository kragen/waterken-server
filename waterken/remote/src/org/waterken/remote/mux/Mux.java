// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.mux;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.db.DatabaseManager;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.store.DoesNotExist;
import org.waterken.uri.Path;
import org.waterken.uri.URI;

/**
 * Puts the persistent databases into the URI hierarchy.
 */
public final class
Mux<S> extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final String prefix;
    private final File root;
    private final DatabaseManager<S> dbm;
    private final Remoting<S> remoting;
    private final Server next;
    
    /**
     * Constructs an instance.
     * @param prefix    URI sub-hierarchy for persistent databases
     * @param root      root persistence folder
     * @param dbm       open vat pool
     * @param remoting  remoting protocol
     * @param next      default server
     */
    public @deserializer
    Mux(@name("prefix") final String prefix,
        @name("root") final File root,
        @name("dbm") final DatabaseManager<S> dbm,
        @name("remoting") final Remoting<S> remoting,
        @name("next") final Server next) {
        this.prefix = prefix;
        this.root = root;
        this.dbm = dbm;
        this.remoting = remoting;
        this.next = next;
    }

    // org.waterken.http.Server interface

    public void
    serve(final String scheme, final Request head,
          final InputStream body, final Client client) throws Exception {
        final Server server;
        final String path = URI.path(head.uri);
        if (path.startsWith(prefix)) {
            final String vatPath = path.substring(prefix.length());
            try {
                final File folder = descend(root, vatPath);
                server = remoting.remote(next, dbm.connect(folder));
            } catch (final InvalidFilenameException e) {
                client.receive(Response.gone(), null);
                return;
            } catch (final DoesNotExist e) {
                client.receive(Response.gone(), null);
                return;
            }
        } else {
            server = next;
        }
        server.serve(scheme, head, body, client);
    }
    
    /**
     * Walks down a file path.
     * @param root  root folder
     * @param path  canonicalized path to walk
     * @return named file
     * @throws InvalidFilenameException invalid name in <code>path</code> 
     */
    static public File
    descend(final File root, final String path) throws InvalidFilenameException{
        File r = root;
        for (final String segment : Path.walk(path)) {
            if (segment.startsWith(".")) {throw new InvalidFilenameException();}
            r = Filesystem.file(r, segment);
        }
        return r;
    }
}
