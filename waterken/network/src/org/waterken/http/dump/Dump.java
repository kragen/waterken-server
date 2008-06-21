// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.dump;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.ASCII;
import org.joe_e.file.Filesystem;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.MIME;
import org.waterken.io.FileType;
import org.waterken.io.snapshot.Snapshot;
import org.waterken.uri.Header;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.web_send.Failure;

/**
 * A POST dump.
 */
public final class
Dump extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final String path;
    private final String key;
    private final File folder;
    private final MIME formats;
    private final Server next;

    /**
     * Constructs an instance.
     * @param path      expected URI path
     * @param key       expected request key
     * @param folder    dumped to folder
     * @param formats   each known file type
     * @param next      next server to try
     */
    public @deserializer
    Dump(@name("path") final String path,
         @name("key") final String key,
         @name("folder") final File folder,
         @name("formats") final MIME formats,
         @name("next") final Server next) {
        this.key = key;
        this.path = path;
        this.folder = folder;
        this.formats = formats;
        this.next = next;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final String resource, final Volatile<Request> requestor,
          final Do<Response,?> respond) throws Exception {
        
        // further dispatch the request
        if (!URI.path(resource).equals(path)) {
            next.serve(resource, requestor, respond);
            return;
        }

        // determine the request
        final Request request;
        try {
            request = requestor.cast();
        } catch (final Exception e) {
            respond.reject(e);
            return;
        }
        if (!request.allow(respond, "POST", "OPTIONS", "TRACE")) {
            return;
        }
        
        // further dispatch the request based on the query string
        final String query = URI.query("", resource);
        final String s = Query.arg(null, query, "s");
        final String p = Query.arg(null, query, "p");
        final String m = Query.arg(null, query, "m");
        if (!s.equals(key) || !p.equals("run") || null == m) {
            respond.reject(Failure.notFound());
            return;
        }
        
        // determine the request Media Type
        final String contentType = request.getContentType();
        FileType type = FileType.binary;
        for (final FileType i : formats.known) {
            if (i.name.equalsIgnoreCase(contentType)) {
                type = i;
                break;
            }
        }
        final String name = m + type.ext;

        // write out the request entity
        final File incoming = Filesystem.file(folder, ".incoming");
        final File tmp = Filesystem.file(incoming, name);
        final OutputStream out = Filesystem.writeNew(tmp);
        request.body.writeTo(out);
        out.flush();
        out.close();
        final File committed = Filesystem.file(folder, name);
        committed.delete();
        if (!tmp.renameTo(committed)) { throw new IOException(); }
        
        // acknowledge the request
        final ByteArray content = ByteArray.array(ASCII.encode("[ null ]\n"));
        respond.fulfill(new Response("HTTP/1.1", "200", "OK",
            PowerlessArray.array(
                new Header("Content-Length", "" + content.length()),
                new Header("Content-Type", "application/jsonrequest")
            ), new Snapshot(content)));
    }
}
