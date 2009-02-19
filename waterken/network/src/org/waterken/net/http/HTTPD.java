// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import org.joe_e.inert;
import org.joe_e.array.PowerlessArray;
import org.joe_e.var.Milestone;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Receiver;
import org.ref_send.promise.Task;
import org.waterken.http.Server;
import org.waterken.http.TokenList;
import org.waterken.io.bounded.Bounded;
import org.waterken.net.TCPDaemon;
import org.waterken.uri.Header;

/**
 * HTTP service daemon.
 */
public final class
HTTPD extends TCPDaemon {
    static private final long serialVersionUID = 1L;
    
    /**
     * connection socket timeout
     */
    public final int soTimeout;
    
    /**
     * request server
     */
    protected final Server server;
    
    /**
     * Constructs an instance.
     * @param port      {@link #port}
     * @param backlog   {@link #backlog}
     * @param SSL       {@link #SSL}
     * @param soTimeout {@link #soTimeout}
     * @param server    request server
     */
    public @deserializer
    HTTPD(@name("port") final int port,
          @name("backlog") final int backlog,
          @name("SSL") final boolean SSL,
          @name("soTimeout") final int soTimeout,
          @name("server") final Server server) {
        super(port, backlog, SSL);
        this.soTimeout = soTimeout;
        this.server = server;
    }
    
    // org.waterken.net.Daemon interface

    public Task<Void>
    accept(final String hostname, final Socket socket, final Receiver<?> yield){
        final String location = SSL
            ? (port == 443 ? hostname : hostname + ":" + port)
        : (port == 80 ? hostname : hostname + ":" + port);
        return new ServerSide(this, location, socket, yield);
    }

	/**
	 * Reads HTTP message headers.
	 * @param hin header input stream
     * @return header list
	 * @throws IOException  any I/O problem
     * @throws Exception    any syntax problem
	 */
	static protected PowerlessArray<Header>
	readHeaders(final LineInput hin) throws Exception {
	    PowerlessArray<Header> r = PowerlessArray.array();
	    String line = hin.readln();
	    while (!"".equals(line)) {
	        final int len = line.length();
	
	        // parse the header name
            final int endName = TokenList.skip(
                TokenList.token, TokenList.nothing, line, 0, len);
            if (':' != line.charAt(endName)) { throw new Exception(); }
	        final String name = line.substring(0, endName);
	
	        // parse the header value
	        final int beginValue =
	            TokenList.skip(TokenList.whitespace, TokenList.nothing,
	                           line, endName + 1, len);
	        String value = line.substring(beginValue);
	
	        // check for continuations
	        line = hin.readln();
	        if (line.startsWith(" ") || line.startsWith("\t")) {
	            final StringBuilder buffer = new StringBuilder();
	            buffer.append(value);
	            do {
	                buffer.append(line);
	                line = hin.readln();
	            } while (line.startsWith(" ") || line.startsWith("\t"));
	            value = buffer.toString();
	        }

	        TokenList.vet(TokenList.text, "\r\n", value);
	        r = r.with(new Header(name, value));
	    }
	    return r;
	}

	/**
	 * Creates the input stream for reading a message body.
	 * @param headers      message headers
	 * @param connection   connection input stream
	 * @return message body input stream, or <code>null</code> if none
	 * @throws Exception   indicates a stream format problem
	 */
	static protected InputStream
	input(final PowerlessArray<Header> headers,
	      @inert final InputStream connection) throws Exception {
	    final StringBuilder encodingList = new StringBuilder();
	    for (final Header header : headers) {
	        if (Header.equivalent("Transfer-Encoding", header.name)) {
	            if (0 != encodingList.length()) { encodingList.append(", "); }
	            encodingList.append(header.value);
	        }
	    }
	    @inert InputStream r = null;
	    final PowerlessArray<String> encoding =
	    	TokenList.decode(encodingList.toString());
	    for (int i = encoding.length(); i-- != 0;) {
	        if (Header.equivalent("chunked", encoding.get(i))) {
	            if (i != encoding.length() - 1) {
	                throw new Exception("Bad Transfer-Encoding");
	            }
	            r = new ChunkedInputStream(connection);
	        } else if (Header.equivalent("identity", encoding.get(i))) {
	        } else { throw new Exception("Encoding Not Implemented"); }
	    }
	    if (null == r) {
	        final String contentLength =
	            TokenList.find(null, "Content-Length", headers);
	        if (null != contentLength) {
	            final int length = Integer.parseInt(contentLength);
	            r = Bounded.input(length, connection);
	        } else if (0 != encoding.length()) {
	            // identity encoding
	            r = connection;
	        }
	    }
	    return r;
	}

	/**
	 * Creates a message header with no connection headers.
	 * @param version  HTTP version
	 * @param headers  message headers
	 * @param closing  Should the connection be closed?
	 * @return general and entity headers
	 */
	static protected PowerlessArray<Header>
	forward(final String version, final PowerlessArray<Header> headers,
	        final Milestone<Boolean> closing) {
	    PowerlessArray<Header> r = PowerlessArray.array();
	    final StringBuilder connectionList = new StringBuilder();
	    for (final Header header : headers) {
	        if (Header.equivalent("Connection", header.name)) {
	            if (0 != connectionList.length()) {connectionList.append(", ");}
	            connectionList.append(header.value);
	        } else if (Header.equivalent("Transfer-Encoding", header.name)) {
	            // already handled by call to body()
	        } else {
	            r = r.with(header);
	        }
	    }
	    boolean keepAlive = false;
	    for (final String token : TokenList.decode(connectionList.toString())) {
	        if (Header.equivalent("close", token)) {
	            closing.mark(true);
	        } else if (Header.equivalent("keep-alive", token)) {
	            keepAlive = true;
	        }
	        for (int i = r.length(); 0 != i--;) {
	            if (Header.equivalent(token, r.get(i).name)) {
	                r = r.without(i);
	            }
	        }
	    }
	    if (!version.startsWith("HTTP/1.")) { closing.mark(true); }
	    if (!keepAlive && "HTTP/1.0".equals(version)) { closing.mark(true); }
	    return r;
	}
}
