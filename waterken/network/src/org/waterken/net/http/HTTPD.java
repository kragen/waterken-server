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

    /**
     * URI scheme
     */
    public    final String scheme;
    protected final Server server;
    protected final Execution exe;
    
    /**
     * Constructs an instance.
     * @param port      {@link #port}
     * @param backlog   {@link #backlog}
     * @param SSL       {@link #SSL}
     * @param soTimeout {@link #soTimeout}
     * @param scheme    {@link #scheme}
     * @param server    request server
     * @param exe       thread control
     */
    public @deserializer
    HTTPD(@name("port") final int port,
          @name("backlog") final int backlog,
          @name("SSL") final boolean SSL,
          @name("soTimeout") final int soTimeout,
          @name("scheme") final String scheme,
          @name("server") final Server server,
          @name("exe") final Execution exe) {
        super(port, backlog, SSL);
        this.soTimeout = soTimeout;
        this.scheme = scheme;
        this.server = server;
        this.exe = exe;
    }
    
    // org.waterken.net.Daemon interface

    public Task
    accept(final String hostname, final Socket socket) {
        final String origin = SSL
            ? (port == 443 ? hostname : hostname + ":" + port)
        : (port == 80 ? hostname : hostname + ":" + port);
        return new Session(this, origin, socket);
    }
}
