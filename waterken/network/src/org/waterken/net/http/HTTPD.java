// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;

import org.joe_e.array.PowerlessArray;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Server;
import org.waterken.http.TokenList;
import org.waterken.io.bounded.Bounded;
import org.waterken.net.Execution;
import org.waterken.net.TCPDaemon;
import org.waterken.uri.Header;
import org.web_send.Failure;

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

    public Task
    accept(final Execution exe, final String hostname, final Socket socket) {
        final String scheme = SSL ? "https" : "http";
        final String origin = SSL
            ? (port == 443 ? hostname : hostname + ":" + port)
        : (port == 80 ? hostname : hostname + ":" + port);
        return new Session(this, exe, scheme, origin, socket);
    }

	/**
	 * Reads HTTP message headers.
	 * @param header    header list to fill
	 * @param hin       header input stream
	 * @throws IOException  any I/O problem
	 */
	static protected void
	readHeaders(final ArrayList<Header> header,
	            final LineInput hin) throws IOException {
	    String line = hin.readln();
	    while (!"".equals(line)) {
	        final int len = line.length();
	
	        // parse the header name
	        final int endName = line.indexOf(':');
	        final String name = line.substring(0, endName);
	
	        // parse the header value
	        final int beginValue = TokenList.skip(" \t", line, endName+1, len);
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
	
	        header.add(new Header(name, value));
	    }
	}

	/**
	 * Creates the input stream for reading a message body from either a request
	 * or a response.
	 * @param header    message headers
	 * @param cin       connection input stream
	 * @return message body input stream, or <code>null</code> if none
	 * @throws Failure  indicates a stream format problem
	 */
	static protected InputStream
	body(final ArrayList<Header> header, final InputStream cin) throws Failure {
	    final StringBuilder encodingList = new StringBuilder();
	    for (Iterator<Header> i = header.iterator(); i.hasNext();) {
	        final Header h = i.next();
	        if ("Transfer-Encoding".equalsIgnoreCase(h.name)) {
	            if (0 != encodingList.length()) { encodingList.append(", "); }
	            encodingList.append(h.value);
	            i.remove();
	        }
	    }
	    InputStream entity = null;
	    final PowerlessArray<String> encoding =
	    	TokenList.decode(encodingList.toString());
	    for (int i = encoding.length(); i-- != 0;) {
	        if ("chunked".equalsIgnoreCase(encoding.get(i))) {
	            if (i != encoding.length() - 1) {
	                throw new Failure("400", "Bad Transfer-Encoding");
	            }
	            entity = new ChunkedInputStream(cin);
	        } else if ("identity".equalsIgnoreCase(encoding.get(i))) {
	        } else {
	            throw new Failure("501", "Encoding Not Implemented");
	        }
	    }
	    if (null == entity) {
	        final String contentLength =
	            Header.find(null, header, "Content-Length");
	        if (null != contentLength) {
	            final int length = Integer.parseInt(contentLength);
	            entity = Bounded.input(length, cin);
	        } else if (0 != encoding.length()) {
	            // identity encoding
	            entity = cin;
	        }
	    }
	    return entity;
	}

	/**
	 * Is a persistent connection indicated?
	 * @param version   HTTP version
	 * @param header    message headers
	 * @return <code>true</code> if persistent, else <code>false</code>
	 */
	static protected boolean
	persist(final String version, final ArrayList<Header> header) {
	    final StringBuilder connectionList = new StringBuilder();
	    for (final Iterator<Header> i = header.iterator(); i.hasNext();) {
	        final Header h = i.next();
	        if ("Connection".equalsIgnoreCase(h.name)) {
	            if (0 != connectionList.length()) {connectionList.append(", ");}
	            connectionList.append(h.value);
	            i.remove();
	        }
	    }
	    boolean close = false;
	    boolean keepAlive = false;
	    for (final String token : TokenList.decode(connectionList.toString())) {
	        if ("close".equalsIgnoreCase(token)) {
	            close = true;
	        } else if ("keep-alive".equalsIgnoreCase(token)) {
	            keepAlive = true;
	        }
	        for (Iterator<Header> i = header.iterator(); i.hasNext();) {
	            if (i.next().name.equalsIgnoreCase(token)) { i.remove(); }
	        }
	    }
	    return !close && (keepAlive || "HTTP/1.1".equals(version));
	}
}
