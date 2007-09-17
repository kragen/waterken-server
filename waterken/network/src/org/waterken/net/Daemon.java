// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net;

import java.net.Socket;

import org.ref_send.promise.eventual.Task;

/**
 * A TCP service daemon.
 */
public interface
Daemon {

    /**
     * Creates a session.
     * <p>
     * The caller is responsible for closing the socket.
     * </p>
     * @param socket    incoming TCP connection
     */
    Task
    accept(Socket socket);
}
