// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.dump;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.array.PowerlessArray;
import org.joe_e.file.Filesystem;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.uri.Header;
import org.waterken.uri.Query;
import org.waterken.uri.URI;

/**
 * A POST dump.
 */
public final class
Dump implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final String path;
    private final String key;
    private final File file;
    private final Server next;
    
    private       OutputStream log;

    /**
     * Constructs an instance.
     * @param path  expected URI path
     * @param key   expected request key
     * @param file  dumped to file
     * @param next  next server to try
     */
    public @deserializer
    Dump(@name("path") final String path,
         @name("key") final String key,
         @name("file") final File file,
         @name("next") final Server next) throws IOException {
        this.path = path;
        this.key = key;
        this.file = file;
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
        final String q = Query.arg(null, query, "q");
        if (!key.equals(s) || !"run".equals(q)) {
            client.receive(Response.notFound(), null);
            return;
        }

        // obey any request restrictions
        if (!head.respond(null, client, "POST", "OPTIONS", "TRACE")) { return; }

        // strip off the outer array syntax
        final int length = head.getContentLength();
        final byte[] buffer = new byte[length];
        for (int i = 0; i != length; ) {
            final int n = body.read(buffer, i, length - i);
            if (-1 == n) { throw new EOFException(); }
            i += n;
        }
        int begin = 0;
        while ('[' != buffer[begin++]) {}
        int end = length - 1;
        while (']' != buffer[end--]) {}

        // write out the log entry
        synchronized (this) {
            if (null == log) {
                file.delete();
                log = Filesystem.writeNew(file);
                log.write('[');
            } else {
                log.write(',');
            }
            log.write(buffer, begin, end - begin);
            log.flush();
        }
        
        // acknowledge the request
        client.receive(new Response("HTTP/1.1", "204", "OK",
            PowerlessArray.array(new Header[] {})), null);
    }
}
