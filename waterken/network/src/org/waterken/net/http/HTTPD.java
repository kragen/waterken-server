// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.net.Socket;

import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Server;
import org.waterken.net.Execution;
import org.waterken.net.TCPDaemon;

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
}
