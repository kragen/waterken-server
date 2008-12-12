// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.mirror;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.inert;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Server;
import org.waterken.http.file.Files;
import org.waterken.http.file.Tag;
import org.waterken.io.MIME;
import org.waterken.uri.Path;
import org.waterken.uri.URI;

/**
 * An HTTP mirror site.
 */
public final class
Mirror extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final File root;
    private final Tag tag;
    private final MIME formats;
    
    /**
     * Constructs an instance.
     * @param root      root folder
     * @param tag       ETag generator
     * @param formats   each known file type
     */
    public @deserializer
    Mirror(@name("root") @inert final File root,
           @name("tag") @inert final Tag tag,
           @name("formats") final MIME formats) {
        this.root = root;
        this.tag = tag;
        this.formats = formats;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final String resource, final Request head, final InputStream body,
                                 final Client client) throws Exception {        
        final File folder;
        try {
            folder = Path.descend(root, Path.folder(URI.path(resource)));
        } catch (final InvalidFilenameException e) {
            client.failed(e);
            return;
        }
        new Files(folder, tag, formats).serve(resource, head, body, client);
    }
}
