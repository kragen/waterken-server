// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.ASCII;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.MediaType;
import org.waterken.io.stream.Stream;
import org.waterken.uri.Filename;
import org.waterken.uri.Header;
import org.waterken.uri.Path;
import org.waterken.uri.URI;
import org.web_send.Failure;

/**
 * An HTTP file server.
 */
public final class
Files {

    private
    Files() {}

    /**
     * Constructs an instance.
     * @param maxAge    max-age value
     * @param tag       ETag generator
     * @param folder    folder
     * @param MIME      each known file type
     */
    static public Server
    make(final int maxAge, final Tag tag,
         final File folder, final PowerlessArray<MediaType> MIME) {
        class ServerX extends Struct implements Server, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            serve(final String resource,
                  final Volatile<Request> requestor,
                  final Do<Response,?> respond) throws Exception {

                // determine the request
                final Request request;
                try {
                    request = requestor.cast();
                } catch (final Exception e) {
                    respond.reject(e);
                    return;
                }

                // check pre-conditions
                if ("GET".equals(request.method) ||
                    "HEAD".equals(request.method) ||
                    "OPTIONS".equals(request.method) ||
                    "TRACE".equals(request.method)) {
                    request.expectContinue(respond);
                }
                
                // we made it to the final processor, so bounce a TRACE
                if ("TRACE".equals(request.method)) {
                    respond.fulfill(request.trace());
                    return;
                }
                
                if (!("GET".equals(request.method) ||
                      "HEAD".equals(request.method))) {
                  respond.fulfill(request.respond("TRACE, OPTIONS, GET, HEAD"));
                  return;
                }

                // check that there is no query
                if (null != URI.query(null, resource)) {
                    respond.reject(Failure.notFound());
                    return;
                }

                // check that folder still exists
                if (!folder.isDirectory()) {
                    respond.reject(Failure.gone());
                    return;
                }

                // extract the filename
                String name = Path.name(URI.path(resource));
                if ("".equals(name)) {
                    name = "index";
                } else if (name.startsWith(".")) {
                    respond.reject(Failure.notFound());
                    return;
                }

                // determine the file
                String ext = Filename.ext(name);
                MediaType contentType = MediaType.binary;
                File file = null;
                try {
                    if ("".equals(ext)) {
                        for (final MediaType x : MIME) {
                            final File f = Filesystem.file(folder, name+x.ext);
                            if (f.isFile()) {
                                ext = x.ext;
                                name += ext;
                                contentType = x;
                                file = f;
                                break;
                            }
                        }
                    } else {
                        final File f = Filesystem.file(folder, name);
                        if (f.isFile()) {
                            for (final MediaType x : MIME) {
                                if (x.ext.equals(ext)) {
                                    contentType = x;
                                    break;
                                }
                            }
                            file = f;
                        }
                    }
                    if (null == file) {

                        // check for a sub-folder
                        final File sub = Filesystem.file(folder, name);
                        if (sub.isDirectory()) {
                            respond.fulfill(new Response(
                                "HTTP/1.1", "303", "See Other",
                                PowerlessArray.array(
                                    new Header("Location", resource + "/"),
                                    new Header("Content-Length", "0")
                                ), null));
                            return;
                        }

                        // check for a redirect
                        final String x = Filename.key(name) + MediaType.uri.ext;
                        final File redirect = Filesystem.file(folder, x);
                        if (redirect.isFile()) {
                            ext = MediaType.uri.ext;
                            name = x;
                            contentType = MediaType.uri;
                            file = redirect;
                        } else {
                            respond.reject(Failure.notFound());
                            return;
                        }
                    }
                } catch (final InvalidFilenameException e) {
                    respond.reject(Failure.notFound());
                    return;
                }
                
                // check for a redirect of any method
                if (MediaType.uri.equals(contentType)) {

                    // load the redirect URI
                    String location = null;
                    final BufferedReader in = new BufferedReader(
                        ASCII.input(Filesystem.read(file)));
                    try {
                        while (true) {
                            final String line = in.readLine();
                            if (null == line) { break; }
                            if (!line.startsWith("#")) {
                                location = URI.resolve(resource, line);
                                break;
                            }
                        }
                    } catch (final Exception e) {
                        in.close();
                        throw e;
                    }
                    in.close();
                    if (null == location) {
                        respond.reject(Failure.notFound());
                        return;
                    }
                    respond.fulfill(new Response(
                        "HTTP/1.1", "303", "See Other",
                        PowerlessArray.array(
                            new Header("Location", location),
                            new Header("Content-Length", "0")
                        ), null));
                    return;
                }

                // support conditional GET
                final String etag = tag.run(file);
                if (request.hasVersion(etag)) {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "304", "Not Modified",
                        PowerlessArray.array(
                            new Header("ETag", etag),
                            new Header("Cache-Control", "max-age=" + maxAge)
                        ), null));
                    return;
                }

                // produce the corresponding representation
                final InputStream in = Filesystem.read(file);
                try {
                    PowerlessArray<Header> header = PowerlessArray.array(
                        new Header("ETag", etag),
                        new Header("Cache-Control", "max-age=" + maxAge),
                        new Header("Content-Length",
                                   "" + Filesystem.length(file)),
                        new Header("Content-Type", contentType.name)
                    );
                    if (null != contentType.encoding) {
                        header = header.with(new Header(
                                "Content-Encoding", contentType.encoding));
                    }
                    respond.fulfill(new Response(
                        "HTTP/1.1", "200", "OK", header,
                        "HEAD".equals(request.method) ? null : new Stream(in)));
                } catch (final Exception e) {
                    try { in.close(); } catch (final Exception e2) {}
                    throw e;
                }
                in.close();
            }
        }
        return new ServerX();
    }
}
