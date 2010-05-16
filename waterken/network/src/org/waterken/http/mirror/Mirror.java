// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.mirror;

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
import org.waterken.http.TokenList;
import org.waterken.io.FileType;
import org.waterken.io.MIME;
import org.waterken.uri.Filename;
import org.waterken.uri.Header;
import org.waterken.uri.Path;
import org.waterken.uri.Query;
import org.waterken.uri.URI;

/**
 * An HTTP mirror site.
 */
public final class
Mirror extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final Archive archive;
    private final MIME formats;
    
    /**
     * Constructs an instance.
     * @param archive   file archive
     * @param formats   each known file type
     */
    public @deserializer
    Mirror(@name("archive") final Archive archive,
           @name("formats") final MIME formats) {
        this.archive = archive;
        this.formats = formats;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final String scheme, final Request head,
          final InputStream body, final Client client) throws Exception {

        // determine the request target
        FileType type = FileType.unknown;
        String encoding;
        Archive.Entry target; {
            final boolean gz = TokenList.includes("gzip",
                    TokenList.list("Accept-Encoding", head.headers));
            final String path = URI.path(head.uri);
            final String name = Path.name(path);
            final String ext = Filename.ext(name);
            if ("".equals(ext)) {
                if (!"".equals(path)) {
                    final Archive.Entry dir = archive.find(path + "/");
                    if (null != dir) {
                        client.receive(new Response(
                            "HTTP/1.1", "307", "Temporary Redirect",
                            PowerlessArray.array(
                                new Header("Location", head.uri + "/"),
                                new Header("Content-Length", "0")
                            )), null);
                        return;
                    }
                }
                encoding = null;
                target = null;
                final String pathname =
                    Path.folder(path) + ("".equals(name) ? "index" : name);
                for (final FileType t : formats.known) {
                    Archive.Entry x =
                        gz && t.z ? archive.find(pathname + t.ext+".gz") : null;
                    if (null != x) {
                        encoding = "gzip";
                    } else {
                        x = archive.find(pathname + t.ext);
                    }
                    if (null != x) {
                        target = x;
                        type = t;
                        break;
                    }
                }
            } else {
                for (final FileType format : formats.known) {
                    if (Header.equivalent(format.ext, ext)) {
                        type = format;
                        break;
                    }
                }
                target = type.z && gz ? archive.find(path + ".gz") : null;
                if (null != target) {
                    encoding = "gzip";
                } else {
                    encoding = null;
                    target = archive.find(path);
                }
            }
        }
        if (null == target) {
            client.receive(Response.notFound(), null);
            return;
        }

        // obey any request restrictions
        final String etag = target.getETag();
        if (!head.respond(etag,client,"GET","HEAD","OPTIONS","TRACE")) {return;}

        // output the corresponding representation
        final String promise = Query.arg(null, URI.query("", head.uri), "o");
        final InputStream in = target.open();
        try {
            final PowerlessArray.Builder<Header> headers =
                PowerlessArray.builder(6);
            headers.append(new Header("ETag", etag));
            headers.append(new Header("Cache-Control",
                "must-revalidate, max-age=" + (null != promise ? forever : 0)));
            headers.append(new Header("Content-Length", ""+target.getLength()));
            headers.append(new Header("Content-Type", type.name));
            if (null != encoding) {
                headers.append(new Header("Content-Encoding", encoding));
                headers.append(new Header("Vary", "Accept-Encoding"));
            }
            client.receive(new Response("HTTP/1.1", "200", "OK",
                headers.snapshot()), "HEAD".equals(head.method) ? null : in);
        } catch (final Exception e) {
            try { in.close(); } catch (final Exception e2) {}
            throw e;
        }
        in.close();
    }
}
