// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.mirror;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.inert;
import org.joe_e.array.PowerlessArray;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.archive.dir.Directory;
import org.waterken.archive.dir.FileMetadata;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.http.file.Files;
import org.waterken.io.MIME;
import org.waterken.uri.Header;
import org.waterken.uri.Path;
import org.waterken.uri.URI;

/**
 * An HTTP mirror site.
 */
public final class
Mirror extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final File root;
    private final FileMetadata meta;
    private final MIME formats;
    
    /**
     * Constructs an instance.
     * @param root      root folder
     * @param meta      ETag generator
     * @param formats   each known file type
     */
    public @deserializer
    Mirror(@name("root") @inert final File root,
           @name("meta") @inert final FileMetadata meta,
           @name("formats") final MIME formats) {
        this.root = root;
        this.meta = meta;
        this.formats = formats;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final Request head, final InputStream body,
                              final Client client) throws Exception {        
        final File folder;
        final File file;
        try {
            final String path = URI.path(head.uri);
            folder = Path.descend(root, Path.folder(path));
            final String name = Path.name(path);
            if (name.startsWith(".")) { throw new InvalidFilenameException(); }
            file = "".equals(name) ? null : Filesystem.file(folder, name);
        } catch (final InvalidFilenameException e) {
            client.receive(Response.gone(), null);
            return;
        }
        if (null != file && file.isDirectory()) {
            client.receive(new Response(
                "HTTP/1.1", "307", "Temporary Redirect",
                PowerlessArray.array(
                    new Header("Location", head.uri + "/"),
                    new Header("Content-Length", "0")
                )), null);
        } else {
            new Files(new Directory(folder, meta), formats).
                serve(head, body, client);
        }
    }
}
