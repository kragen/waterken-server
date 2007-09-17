// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.waterken.net.Daemon;

/**
 * A TCP daemon.
 */
final class
TCP implements Runnable {
    
    private final PrintStream err;
    private final ThreadGroup threads;
    private final String protocol;
    private final Daemon daemon;
    private final int soTimeout;
    private final ServerSocket port;
    
    private       long count = 0;
    
    TCP(final PrintStream err, final ThreadGroup threads,  
        final String protocol, final Daemon daemon,
        final int soTimeout, final ServerSocket port) {
        
        this.err = err;
        this.threads = threads;
        this.protocol = protocol;
        this.daemon = daemon;
        this.soTimeout = soTimeout;
        this.port = port;
    }

    public void
    run() {
        err.println("Running " + protocol +
                    " at " + port.getLocalSocketAddress() + " ...");
        while (true) {
            final Socket socket;
            try {
                socket = port.accept();
            } catch (final IOException e) {
                // Something strange happened.
                e.printStackTrace();
                continue;
            }
            final String name = "" + count++;
            new Thread(threads, new Runnable() {
                public void
                run() {
                    final String prefix = threads.getName() + "." + name;
                    try {
                        err.println(prefix + ": processing...");
                        socket.setSoTimeout(soTimeout);
                        daemon.accept(socket).run();
                    } catch (final Exception e) {
                        err.println(prefix + ": " + e.getMessage());
                    } finally {
                        try { socket.close(); } catch (final Exception e) {}
                    }
                }
            }, name).start();
        }
    }
}
