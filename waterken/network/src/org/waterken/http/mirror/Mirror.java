// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.mirror;

import java.io.File;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.http.file.Files;
import org.waterken.http.file.Tag;
import org.waterken.io.MIME;
import org.waterken.uri.Path;
import org.waterken.uri.URI;
import org.web_send.Failure;

/**
 * An HTTP mirror site.
 */
public final class
Mirror extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final int maxAge;
    private final Tag tag;
    private final File root;
    private final MIME formats;
    
    /**
     * Constructs an instance.
     * @param maxAge    max-age value
     * @param tag       ETag generator
     * @param root      root folder
     * @param formats   each known file type
     */
    public @deserializer
    Mirror(@name("maxAge") final int maxAge,
           @name("tag") final Tag tag,
           @name("root") final File root,
           @name("formats") final MIME formats) {
        this.maxAge = maxAge;
        this.tag = tag;
        this.root = root;
        this.formats = formats;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final String resource, final Volatile<Request> request,
          final Do<Response,?> respond) throws Exception {
        final File f;
        try {
            f = Path.descend(root, URI.path(resource));
        } catch (final InvalidFilenameException e) {
            respond.reject(Failure.gone());
            return;
        }
        new Files(maxAge, tag, f, formats).serve(resource, request, respond);
    }
}
