// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import static org.joe_e.array.PowerlessArray.array;
import static org.waterken.io.Stream.chunkSize;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

import org.joe_e.charset.ASCII;
import org.joe_e.var.Milestone;
import org.ref_send.promise.eventual.Do;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.http.TokenList;
import org.waterken.io.Stream;
import org.waterken.io.bounded.Bounded;
import org.waterken.io.open.Open;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * Outputs a {@link Response}.  
 */
final class
Responder {

    public  final Milestone<Boolean> closing = Milestone.plan();
    private final Server server;
    private       OutputStream connection;

    Responder(final Server server) {
        this.server = server;
    }

    Responder(final Server server, final OutputStream connection) {
        this.server = server;
        this.connection =
            new BufferedOutputStream(connection,chunkSize-"0\r\n\r\n".length());
    }
    
    protected Do<Response,Void>
    respond(final String version, final String method, final Responder next) {
        return new Do<Response,Void>() {

            public Void
            fulfill(final Response response) throws Exception {
                if (null == connection) { throw new Exception(); }
                final OutputStream out = connection;
                connection = null;
                try {
                    write(closing, out, version, method, response);
                } catch (final Exception e) {
                    closing.mark(true);
                    try { out.close(); } catch (final IOException e2) {}
                    throw e;
                }
                out.flush();
                if (closing.is()) {
                    out.close();
                } else {
                    if (response.head.status.startsWith("1")) {
                        connection = out;
                    } else {
                        next.connection = out;
                    }
                }
                return null;
            }

            public Void
            reject(final Exception reason) throws Exception {
                final String status;
                final String phrase;
                if (reason instanceof FileNotFoundException) {
                    status = "404";
                    phrase = "Not Found";
                } else if (reason instanceof EOFException) {
                    status = "413";
                    phrase = "Request Entity Too Large";
                } else {
                    status = "400";
                    phrase = "Bad Request";
                }
                final Do<Response,Void> m = this;
                final String resource = URI.resolve("file:///site/", status); 
                server.serve(resource, new Request(new Request.Head(
                                 version, "GET", resource,
                                 array(new Header[] {})), null),
                             new Do<Response,Void>() {

                    public Void
                    fulfill(final Response response) throws Exception {
                        return m.fulfill(new Response(new Response.Head(
                            response.head.version, status, phrase,
                            response.head.headers), response.body));
                    }

                    public Void
                    reject(final Exception reason) throws Exception {
                        return m.fulfill(new Response(new Response.Head(
                            "HTTP/1.1", status, phrase,
                            array(new Header("Content-Length", "0"))), null));
                    }
                });
                return null;
            }
        };
    }
    
    /**
     * Writes an HTTP response.
     * <p>
     * If the connection is to be kept alive, the implementation MUST write a
     * complete response and prevent further writes to the output stream by the
     * <code>response</code>. If the connection is not to be kept alive, the
     * method MUST either set the closing flag, or throw an exception. 
     * </p>
     */
    static private void
    write(final Milestone<Boolean> closing, final OutputStream out,
          final String version, final String method,
          final Response response) throws Exception {
        final boolean empty =
            "HEAD".equals(method) ||
            response.head.status.startsWith("1") ||
            "204".equals(response.head.status) ||
            "205".equals(response.head.status) ||
            "304".equals(response.head.status);
        if (empty && null != response.body) { throw new Exception(); }
        if (response.head.status.startsWith("1") &&
            !"100".equals(response.head.status)) { throw new Exception(); }
        if (response.head.status.equals("306")) { throw new Exception(); }
        if (response.head.status.equals("402")) { throw new Exception(); }
        if (response.head.status.length() != 3) { throw new Exception(); }
        final char major = response.head.status.charAt(0);
        if ('1' > major || '5' < major) { throw new Exception(); }
        for (int i = response.head.status.length(); --i != 0;) {
            final char c = response.head.status.charAt(i);
            if ('0' > c || '9' < c) { throw new Exception(); }
        }
        TokenList.vet(TokenList.text, "\r\n", response.head.phrase);

        // output the Response-Line
        final Writer hrs = ASCII.output(Open.output(out));
        hrs.write("HTTP/1.1 ");
        hrs.write(response.head.status);
        hrs.write(" ");
        hrs.write(response.head.phrase);
        hrs.write("\r\n");

        // output the header
        final Milestone<Boolean> selfDelimiting = Milestone.plan();
        if (empty) { selfDelimiting.mark(true); }
        final Milestone<Boolean> contentLengthSpecified = Milestone.plan();
        long contentLength = 0;
        for (final Header header : response.head.headers) {
            if (!contentLengthSpecified.is() &&
                    TokenList.equivalent("Content-Length", header.name)) {
                contentLengthSpecified.mark(true);
                if (!"HEAD".equals(method)) {
                    contentLength = Long.parseLong(header.value);
                    if (0 > contentLength) {throw new Exception("Bad Length");}
                    selfDelimiting.mark(true);
                }
            } else {
                for (final String name : new String[] { "Content-Length",
                                                        "Connection",
                                                        "Transfer-Encoding",
                                                        "TE",
                                                        "Trailer",
                                                        "Upgrade" }) {
                    if (TokenList.equivalent(name, header.name)) {
                        throw new Exception("Illegal response header");
                    }
                }
            }
            TokenList.vet(TokenList.token, "", header.name);
            TokenList.vet(TokenList.text, "\r\n", header.value);

            hrs.write(header.name);
            hrs.write(": ");
            hrs.write(header.value);
            hrs.write("\r\n");
        }
        
        // complete the response
        if ("HTTP/1.1".equals(version)) {
            if (selfDelimiting.is()) {
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                final OutputStream brs = Bounded.output(contentLength, out);
                if (null != response.body) { Stream.copy(response.body, brs); }
                brs.close();
            } else {
                hrs.write("Transfer-Encoding: chunked\r\n");
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                final OutputStream brs = new ChunkedOutputStream(chunkSize,out);
                if (null != response.body) { Stream.copy(response.body, brs); }
                brs.close();
            }
        } else {
            if (selfDelimiting.is()) {
                if (closing.is()) {
                    hrs.write("Connection: close\r\n");
                } else {
                    hrs.write("Connection: keep-alive\r\n");
                }
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                final OutputStream brs = Bounded.output(contentLength, out);
                if (null != response.body) { Stream.copy(response.body, brs); }
                brs.close();
            } else {
                closing.mark(true);
                hrs.write("Connection: close\r\n");
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                if (null != response.body) { Stream.copy(response.body, out); }
            }
        }
    }
}
