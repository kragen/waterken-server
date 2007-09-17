// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.Serializable;
import java.net.Socket;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Server;
import org.waterken.net.Daemon;
import org.waterken.net.Execution;

/**
 * HTTP service daemon.
 */
public final class
HTTPD {

    private
    HTTPD() {}

    /**
     * Constructs an instance. 
     * @param scheme    URI scheme
     * @param server    request server
     * @param thread    thread control
     */
    static public Daemon
    make(final String scheme, final Server server, final Execution thread) {
        class DaemonX extends Struct implements Daemon, Serializable {
            static private final long serialVersionUID = 1L;

            public Task
            accept(final Socket socket) {
                return new Session(scheme, server, thread, socket);
            }
        }
        return new DaemonX();
    }
}
