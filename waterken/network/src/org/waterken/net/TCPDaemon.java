// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net;

import java.io.Serializable;
import java.net.Socket;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;

/**
 * A TCP service daemon.
 */
public abstract class
TCPDaemon extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * server port to listen on
     */
    public final int port;
    
    /**
     * server port listen backlog
     */
    public final int backlog;
    
    /**
     * SSL in use
     */
    public final boolean SSL;
    
    protected
    TCPDaemon(final int port, final int backlog, final boolean SSL) {
        this.port = port;
        this.backlog = backlog;
        this.SSL = SSL;
    }

    /**
     * Creates a session.
     * <p>
     * The caller is responsible for closing the socket.
     * </p>
     * @param yield     yield to other threads
     * @param hostname  server's hostname
     * @param socket    incoming TCP connection
     */
    public abstract Task<Void>
    accept(Receiver<Void> yield, String hostname, Socket socket);
}
