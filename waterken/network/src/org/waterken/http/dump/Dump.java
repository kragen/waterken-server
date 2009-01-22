// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.dump;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.joe_e.file.Filesystem;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.FileType;
import org.waterken.io.MIME;
import org.waterken.io.Stream;
import org.waterken.uri.Header;
import org.waterken.uri.Query;
import org.waterken.uri.URI;

/**
 * A POST dump.
 */
public final class
Dump extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final String path;
    private final String key;
    private final File folder;
    private final MIME supported;
    private final Server next;

    /**
     * Constructs an instance.
     * @param path      expected URI path
     * @param key       expected request key
     * @param folder    dumped to folder
     * @param supported each supported file type
     * @param next      next server to try
     */
    public @deserializer
    Dump(@name("path") final String path,
         @name("key") final String key,
         @name("folder") final File folder,
         @name("supported") final MIME supported,
         @name("next") final Server next) {
        this.path = path;
        this.key = key;
        this.folder = folder;
        this.supported = supported;
        this.next = next;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final Request head, final InputStream body,
                              final Client client) throws Exception {        
        // further dispatch the request
        if (!URI.path(head.uri).equals(path)) {
            next.serve(head, body, client);
            return;
        }

        // further dispatch the request based on the query string
        final String query = URI.query("", head.uri);
        final String s = Query.arg(null, query, "s");
        final String p = Query.arg(null, query, "p");
        final String m = Query.arg(null, query, "m");
        if (!s.equals(key) || !p.equals("run") || null == m) {
            client.run(Response.notFound(), null);
            return;
        }
        
        // determine the request Media Type
        final String contentTypeSpec = head.getContentType();
        FileType contentType = null;
        for (final FileType format : supported.known) {
            if (Header.equivalent(format.name, contentTypeSpec)) {
                contentType = format;
                break;
            }
        }
        if (null == contentType) {
            client.run(new Response(
                "HTTP/1.1", "415", "Unsupported Media Type",
                PowerlessArray.array(
                    new Header("Content-Length", "0")
                )), null);
            return;
        }

        // obey any request restrictions
        if (!head.respond(null, client, "POST", "OPTIONS", "TRACE")) { return; }

        // write out the request entity
        final String filename = m + contentType.ext;
        final File incoming = Filesystem.file(folder, ".incoming");
        final File tmp = Filesystem.file(incoming, filename);
        final OutputStream out = Filesystem.writeNew(tmp);
        Stream.copy(body, out);
        out.flush();
        out.close();
        final File committed = Filesystem.file(folder, filename);
        committed.delete();
        if (!tmp.renameTo(committed)) { throw new IOException(); }
        
        // acknowledge the request
        client.run(new Response("HTTP/1.1", "204", "OK",
            PowerlessArray.array(new Header[] {})), null);
    }
}
