// Copyright 2002-2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.file;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.archive.Archive;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.FileType;
import org.waterken.io.MIME;
import org.waterken.uri.Filename;
import org.waterken.uri.Header;
import org.waterken.uri.Path;
import org.waterken.uri.Query;
import org.waterken.uri.URI;

/**
 * An HTTP file server.
 */
public final class
Files extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final Archive archive;
    private final MIME formats;
    
    /**
     * Constructs an instance.
     * @param archive   files to serve
     * @param formats   each known file type
     */
    public @deserializer
    Files(@name("archive") final Archive archive,
          @name("formats") final MIME formats) {
        this.archive = archive;
        this.formats = formats;
    }
    
    public void
    serve(final Request head,
          final InputStream body, final Client client) throws Exception {        

        // determine the request target
        FileType contentType = FileType.unknown;
        String etag;
        String filename;
        try {
            final String pathName = Path.name(URI.path(head.uri));
            final String name = "".equals(pathName) ? "index" : pathName;
            final String ext = Filename.ext(name);
            final String exact = archive.tag(name);
            if (null != exact) {
                filename = name;
                etag = exact;
                for (final FileType format : formats.known) {
                    if (Header.equivalent(format.ext, ext)) {
                        contentType = format;
                        break;
                    }
                }
            } else if ("".equals(ext)) {
                filename = null;
                etag = null;
                for (final FileType format : formats.known) {
                    final String candidate = name + format.ext;
                    final String found = archive.tag(candidate);
                    if (null != found) {
                        filename = candidate;
                        etag = found;
                        contentType = format;
                        break;
                    }
                }
                if (null == filename) { throw new FileNotFoundException(); }
            } else { throw new FileNotFoundException(); }
        } catch (final FileNotFoundException e) {
            client.receive(Response.notFound(), null);
            return;
        }

        // obey any request restrictions
        if (!head.respond(etag,client,"GET","HEAD","OPTIONS","TRACE")) {return;}

        // output the corresponding representation
        final String promise = Query.arg(null, URI.query("", head.uri), "o");
        final InputStream in = archive.read(filename);
        try {
            PowerlessArray<Header> header = PowerlessArray.array(
                new Header("ETag", etag),
                new Header("Cache-Control",
                           null != promise ? "max-age=" + forever : "no-cache"),
                new Header("Content-Length", "" + archive.measure(filename)),
                new Header("Content-Type", contentType.name)
            );
            if (null != contentType.encoding) {
                header = header.with(new Header("Content-Encoding",
                                                contentType.encoding));
            }
            client.receive(new Response("HTTP/1.1", "200", "OK", header),
                           "HEAD".equals(head.method) ? null : in);
        } catch (final Exception e) {
            try { in.close(); } catch (final Exception e2) {}
            throw e;
        }
        in.close();
    }
}
