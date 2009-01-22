// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import static org.joe_e.array.PowerlessArray.array;
import static org.waterken.io.Stream.chunkSize;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.ASCII;
import org.joe_e.var.Milestone;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.http.TokenList;
import org.waterken.io.Stream;
import org.waterken.io.bounded.Bounded;
import org.waterken.io.open.Open;
import org.waterken.uri.Header;

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
    
    protected Client
    respond(final String version, final String method, final Responder next) {
        return new Client() {

            public void
            run(final Response head, final InputStream body) throws Exception{
                if (null == body && head.status.startsWith("4")) {
                    // use the configured response body
                    server.serve(new Request(version,"GET","/site/"+head.status,
                            array(new Header[] {})), null, new Client() {
                       public void
                       run(final Response bodyHead,
                           final InputStream body) throws Exception {
                           PowerlessArray<Header> headers = bodyHead.headers;
                           for (final Header i : head.headers) {
                               if (null==TokenList.find(null, i.name, headers)){
                                   headers = headers.with(i);
                               }
                           }
                           output(new Response(head.version, head.status,
                                               head.phrase, headers), body);
                       }
                    });
                } else {
                    output(head, body);
                }
            }
            
            private void
            output(final Response head, final InputStream body)throws Exception{
                if (null == connection) { return; }
                final OutputStream out = connection;
                connection = null;
                try {
                    write(closing, out, version, method, head, body);
                } catch (final Exception e) {
                    closing.mark(true);
                    try { out.close(); } catch (final IOException e2) {}
                    throw e;
                }
                out.flush();
                if (closing.is()) {
                    out.close();
                } else {
                    if (head.status.startsWith("1")) {
                        connection = out;
                    } else {
                        next.connection = out;
                    }
                }
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
          final Response head, final InputStream body) throws Exception {
        final boolean empty =
            "HEAD".equals(method) ||
            head.status.startsWith("1") ||
            "204".equals(head.status) ||
            "205".equals(head.status) ||
            "304".equals(head.status);
        if (empty && null != body) { throw new Exception(); }
        if (head.status.startsWith("1") &&
            !"100".equals(head.status)) { throw new Exception(); }
        if (head.status.equals("306")) { throw new Exception(); }
        if (head.status.equals("402")) { throw new Exception(); }
        if (head.status.length() != 3) { throw new Exception(); }
        final char major = head.status.charAt(0);
        if ('1' > major || '5' < major) { throw new Exception(); }
        for (int i = head.status.length(); --i != 0;) {
            final char c = head.status.charAt(i);
            if ('0' > c || '9' < c) { throw new Exception(); }
        }
        TokenList.vet(TokenList.text, "\r\n", head.phrase);

        // output the Response-Line
        final Writer hrs = ASCII.output(Open.output(out));
        hrs.write("HTTP/1.1 ");
        hrs.write(head.status);
        hrs.write(" ");
        hrs.write(head.phrase);
        hrs.write("\r\n");

        // output the header
        final Milestone<Boolean> selfDelimiting = Milestone.plan();
        if (empty) { selfDelimiting.mark(true); }
        final Milestone<Boolean> contentLengthSpecified = Milestone.plan();
        long contentLength = 0;
        for (final Header header : head.headers) {
            if (!contentLengthSpecified.is() &&
                    Header.equivalent("Content-Length", header.name)) {
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
                    if (Header.equivalent(name, header.name)) {
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
                if (null != body) { Stream.copy(body, brs); }
                brs.close();
            } else {
                hrs.write("Transfer-Encoding: chunked\r\n");
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                final OutputStream brs = new ChunkedOutputStream(chunkSize,out);
                if (null != body) { Stream.copy(body, brs); }
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
                if (null != body) { Stream.copy(body, brs); }
                brs.close();
            } else {
                closing.mark(true);
                hrs.write("Connection: close\r\n");
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                if (null != body) { Stream.copy(body, out); }
            }
        }
    }
}
