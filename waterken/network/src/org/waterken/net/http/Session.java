// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.InputStream;
import java.net.Socket;

import org.joe_e.array.PowerlessArray;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.TokenList;
import org.waterken.io.limited.Limited;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * An HTTP protocol server session.
 */
final class
Session implements Task<Void> {

    private final HTTPD config;
    private final Receiver<Void> yield;
    private final String scheme;
    private final String origin;
    private final Socket socket;

    /**
     * Constructs an instance.
     * <p>
     * The implementation is designed to be defensively consistent with
     * respect to both the remote client and the local
     * {@link HTTPD#server server}. The implementation further attempts to not
     * give the remote client a DOS force multiplier.
     * </p>
     * @param config    configuration
     * @param yield     yield to other threads
     * @param scheme    expected URI scheme
     * @param origin    expected value of the Host header
     * @param socket    connection socket, trusted to behave like a socket, but
     *                  not trusted to be connected to a trusted HTTP client
     */
    Session(final HTTPD config, final Receiver<Void> yield, 
            final String scheme, final String origin, final Socket socket) {
        this.config = config;
        this.yield = yield;
        this.scheme = scheme;
        this.origin = origin;
        this.socket = socket;
    }

    // org.ref_send.promise.eventual.Task interface

    public Void
    run() throws Exception {
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(config.soTimeout);
        final InputStream connection = socket.getInputStream();
        Responder current=new Responder(config.server,socket.getOutputStream());
        while (true) {

            // read the Request-Line
            final LineInput hin =
                new LineInput(Limited.input(32 * 1024, connection));
            final String requestLine = hin.readln();
            final int endRequestLine = requestLine.length();

            // empty line is ignored
            if (endRequestLine == 0) { continue; }

            // parse the Method
            final int beginMethod = 0;
            final int endMethod = TokenList.skip(
                TokenList.token, TokenList.nothing,
                requestLine, beginMethod, endRequestLine);
            if (' ' != requestLine.charAt(endMethod)) { throw new Exception(); }
            final String method = requestLine.substring(beginMethod, endMethod);

            // parse the Request-URI
            final int beginRequestURI = endMethod + 1;
            final int endRequestURI = requestLine.indexOf(' ', beginRequestURI);
            final String requestURI = -1 == endRequestURI
                ? requestLine.substring(beginRequestURI)
            : requestLine.substring(beginRequestURI, endRequestURI);

            // parse the HTTP-Version
            final String version = -1 == endRequestURI
                ? "HTTP/0.9"
            : requestLine.substring(endRequestURI + 1);

            final Responder next = new Responder(config.server);
            final Do<Response,?> respond = current.respond(version,method,next);
            final InputStream body;
            try {
                // parse the request based on the protocol version
                final PowerlessArray<Header> headers;
                if (version.startsWith("HTTP/1.")) {
                    final PowerlessArray<Header> all = HTTPD.readHeaders(hin); 
                    body = HTTPD.input(all, connection);
                    headers = HTTPD.forward(version, all, current.closing);
                } else if (version.startsWith("HTTP/0.")) {
                    // old HTTP client; no headers, no content
                    current.closing.mark(true);
                    headers = PowerlessArray.array();
                    body = null;
                } else {
                    throw new Exception("HTTP Version Not Supported: "+version);
                }
    
                // do some sanity checking on the request
                if (null != body && "TRACE".equals(method)) {
                    throw new Exception("No entity allowed in TRACE");
                }
                if (null == body &&
                        null != TokenList.find(null, "Content-Type", headers)) {
                    throw new Exception("unknown message length");
                }
    
                // determine the request target
                String host = TokenList.find(null, "Host", headers);
                if (null == host) {
                    if (version.startsWith("HTTP/1.") &&
                            !version.equals("HTTP/1.0")){
                        throw new Exception("Missing Host header");
                    }
                    host = "localhost";
                }
                if (!TokenList.equivalent(origin, host)) {
                    // client is hosting this server under the wrong origin
                    // this could lead to a browser side scripting attack
                    throw new Exception("wrong origin");
                }
                final String resource = "*".equals(requestURI)
                    ? "*"
                : URI.resolve(scheme + "://" + origin + "/", requestURI);

                // process the request
                config.server.serve(resource, new Request(new Request.Head(
                    version, method, requestURI, headers), body), respond);
            } catch (final Exception e) {
                current.closing.mark(true);
                respond.fulfill(new Response(new Response.Head(
                    "HTTP/1.1", "503", "Service Unavailable",
                    PowerlessArray.array(
                        new Header("Content-Length", "0")
                    )), null));
                throw e;
            }

            if (current.closing.is()) { break; }
            current = next;

            // ensure the request body is removed from the input stream
            if (null != body) {
                while (body.read() != -1) { body.skip(Long.MAX_VALUE); }
                body.close();
            }

            // now is a good time for a context switch since we're not holding
            // any locks, or much memory
            yield.run(null);
        }
        return null;
    }
}
