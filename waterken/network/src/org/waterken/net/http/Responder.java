// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import static org.waterken.io.Stream.chunkSize;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
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
Responder extends Client {

    private final Server server;
    private final Milestone<Boolean> failed;    // should connection be failed?
    private       OutputStream connection;      // null unless this response is
                                                // next one to output
    
    // response data waiting it's turn for output
    public  final Milestone<Boolean> closing = Milestone.make();
    private       Responder next;
    private       String version;
    private       String method;
    private       Response head;
    private       InputStream body;

    private
    Responder(final Server server, final Milestone<Boolean> failed) {
        this.server = server;
        this.failed = failed;
    }

    Responder(final Server server, final OutputStream connection) {
        this.server = server;
        failed = Milestone.make();
        this.connection =
            new BufferedOutputStream(connection,chunkSize-"0\r\n\r\n".length());
    }
    
    // org.waterken.http.Client interface
    
    public synchronized boolean
    isStillWaiting() { return !failed.is(); }
    
    public synchronized void
    fail(final Exception reason) throws Exception {
        failed.set(true);
        closing.set(true);
        super.fail(reason);
    }
    
    public synchronized void
    receive(final Response head, final InputStream body) throws Exception {
        if (null == body && head.status.startsWith("4")) {
            server.serve("http", new Request(version,"GET","/site/"+head.status,
                   PowerlessArray.array(new Header[0])), null, new Client() {
               public void
               receive(final Response entity,
                       final InputStream body) throws Exception {
                   // merge response headers with default entity headers
                   PowerlessArray<Header> headers = entity.headers;
                   for (final Header i : head.headers) {
                       if (null == TokenList.find(null, i.name, headers)) {
                           headers = headers.with(i);
                       }
                   }
                   setResponse(new Response(head.version, head.status,
                                            head.phrase, headers), body);
               }
            });
        } else {
            setResponse(head, body);
        }
    }
    
    private synchronized void
    setResponse(final Response head, InputStream body) throws Exception {
        if (null != body &&
                !(body instanceof ByteArrayInputStream) && null == connection) {
            // buffer the body until the connection is ready
            final int len = head.getContentLength();
            body = Stream.snapshot(len >= 0 ? len : 512, body).asInputStream();
        }
        this.head = head;
        this.body = body;
        
        final OutputStream out = connection;
        if (null != out) {
            connection = null;
            output(out);
        }
    }
    
    private synchronized void
    output(final OutputStream out) throws Exception {
        if (null != head) { // output already produced response
            try {
                write(closing, out, version, method, head, body);
            } catch (final Exception e) {
                failed.set(true);
                closing.set(true);
                try { out.close(); } catch (final IOException e2) {}
                throw e;
            }
            if (closing.is()) {
                failed.set(true);
                out.flush();
                out.close();
            } else {
                if (head.status.startsWith("1")) {
                    out.flush();
                    connection = out;
                } else {
                    next.output(out);
                }
            }
        } else {            // hold onto output stream until response is ready
            out.flush();
            connection = out;
        }
    }
    
    // org.waterken.net.http.Responder interface
    
    protected synchronized Responder
    follow(final String version, final String method) {
        next = new Responder(server, failed);
        this.version = version;
        this.method = method;
        return next;
    }
    
    /**
     * Writes an HTTP response.
     * <p>
     * If the connection is to be kept alive, the implementation MUST write a
     * complete response. If the connection is not to be kept alive, the
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
        
        hrs.write("Access-Control-Allow-Origin: *\r\n");

        // output the header
        final Milestone<Boolean> selfDelimiting = Milestone.make();
        if (empty) { selfDelimiting.set(true); }
        final Milestone<Boolean> contentLengthSpecified = Milestone.make();
        long contentLength = 0;
        for (final Header header : head.headers) {
            if (!contentLengthSpecified.is() &&
                    Header.equivalent("Content-Length", header.name)) {
                contentLengthSpecified.set(true);
                if (!"HEAD".equals(method)) {
                    contentLength = Long.parseLong(header.value);
                    if (0 > contentLength) {throw new Exception("Bad Length");}
                    selfDelimiting.set(true);
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
                closing.set(true);
                hrs.write("Connection: close\r\n");
                hrs.write("\r\n");
                hrs.flush();
                hrs.close();

                if (null != body) { Stream.copy(body, out); }
            }
        }
    }
}
