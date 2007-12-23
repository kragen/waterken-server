// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import static org.joe_e.array.PowerlessArray.array;
import static org.ref_send.promise.Fulfilled.ref;

import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;

import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.limited.Limited;
import org.waterken.io.stream.Stream;
import org.waterken.net.Execution;
import org.waterken.uri.Header;
import org.waterken.uri.Location;
import org.waterken.uri.URI;
import org.web_send.Failure;

/**
 * An HTTP protocol server session.
 */
final class
Session implements Task {

    private final String scheme;
    private final Server server;
    private final Socket socket;
    private final Execution thread;

    /**
     * Constructs an instance.
     * <p>
     * The implementation is designed to be defensively consistent with
     * respect to both the remote client and the local <code>server</code>. The
     * implementation further attempts to not give the remote client a DOS force
     * multiplier.
     * </p>
     * @param scheme    URL scheme, trusted to be the correct scheme identifier
     * @param server    HTTP server, untrusted
     * @param socket    connection socket, trusted to behave like a socket, but
     *                  not trusted to be connected to a trusted HTTP client
     */
    Session(final String scheme, final Server server,
            final Execution thread, final Socket socket) {
        this.scheme = scheme;
        this.server = server;
        this.thread = thread;
        this.socket = socket;
    }

    // org.ref_send.promise.eventual.Task interface

    public void
    run() throws Exception {
        socket.setTcpNoDelay(true);
        final InputStream cin = socket.getInputStream();
        Responder current = new Responder(server, socket.getOutputStream());
        while (true) {

            // read the Request-Line
            final LineInput hin = new LineInput(Limited.input(32 * 1024, cin));
            final String requestLine = hin.readln();
            final int endRequestLine = requestLine.length();

            // empty line is ignored
            if (endRequestLine == 0) { continue; }

            // parse the Method
            final int beginMethod = 0;
            final int endMethod =
                HTTP.findSP(requestLine, beginMethod, endRequestLine);
            final String method = requestLine.substring(beginMethod, endMethod);

            // parse the Request-URI
            final int beginRequestURI =
                HTTP.skipSP(requestLine, endMethod, endRequestLine);
            final int endRequestURI =
                HTTP.findSP(requestLine, beginRequestURI, endRequestLine);
            final String requestURI =
                requestLine.substring(beginRequestURI, endRequestURI);

            // parse the HTTP-Version
            final int beginHTTPVersion =
                HTTP.skipSP(requestLine, endRequestURI, endRequestLine);
            final String version = beginHTTPVersion == endRequestLine
                ? "HTTP/0.9"
                : requestLine.substring(beginHTTPVersion);

            // parse the request based on the protocol version
            boolean done = false;
            final Responder next = new Responder(server);
            final Do<Response,?> respond = current.respond(version,method,next);
            final InputStream entity;
            try {
                // parse the request
                final ArrayList<Header> header = new ArrayList<Header>(16);
                if (version.startsWith("HTTP/1.")) {
                    HTTP.readHeaders(header, hin);
                    entity = HTTP.body(header, cin);
                } else if (version.startsWith("HTTP/0.")) {
                    // old HTTP client; no headers, no content
                    done = true;
                    current.setClosing();
                    entity = null;
                } else {
                    throw new Failure("505", "HTTP Version Not Supported");
                }
                if (!HTTP.persist(version, header)) {
                    done = true;
                    current.setClosing();
                }
                final Request request = new Request(
                    version, method, requestURI,
                    array(header.toArray(new Header[header.size()])),
                    null != entity ? new Stream(entity) : null);
    
                // do some sanity checking on the request
                if (null != entity && "TRACE".equals(method)) {
                    throw new Failure("400", "No entity allowed in TRACE");
                }
                if (null == entity &&
                        null != Header.find(null, header, "Content-Type")) {
                    throw new Failure("411", "Length Required");
                }
    
                // determine the request target
                String host = Header.find(null, header, "Host");
                if (null == host) {
                    if (version.startsWith("HTTP/1.") &&
                            !version.equals("HTTP/1.0")){
                        throw new Failure("400", "Missing Host header");
                    }
                    host = "localhost";
                } else {
                    Location.vet(host);
                }
                final String resource = "*".equals(requestURI)
                    ? "*"
                : URI.resolve(scheme + "://" + host + "/", requestURI);

                // process the request
                server.serve(resource, ref(request), respond);
            } catch (final Exception e) {
                done = true;
                current.setClosing();
                respond.reject(e instanceof Failure
                    ? e
                : new Failure("500", e.toString()));
                throw e;
            }
            current = next;
            if (done) { break; }

            // ensure the request body is removed from the input stream
            if (null != entity) {
                while (entity.read() != -1) { entity.skip(Long.MAX_VALUE); }
                entity.close();
            }

            // Now is a good time for a context switch since we're not holding
            // any locks, or much memory.
            thread.yield();
        }
    }
}
