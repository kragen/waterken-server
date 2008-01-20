// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.udp;

import java.io.Serializable;
import java.net.SocketAddress;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.eventual.Do;

/**
 * A UDP service daemon.
 */
public abstract class
UDPDaemon extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * server port to listen on
     */
    public final int port;
    
    protected
    UDPDaemon(final int port) {
        this.port = port;
    }

    /**
     * Eventually handle a request.
     * @param from      alleged message sender
     * @param msg       message bytes
     * @param respond   corresponding response processor
     * @throws Exception    any problem
     */
    public abstract void
    accept(SocketAddress from, ByteArray msg,
           Do<ByteArray,?> respond) throws Exception;
}
