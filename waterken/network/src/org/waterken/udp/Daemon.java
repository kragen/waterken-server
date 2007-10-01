// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.udp;

import java.net.SocketAddress;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.eventual.Do;

/**
 * A UDP service daemon.
 */
public interface
Daemon {

    /**
     * Eventually handle a request.
     * @param from      alleged message sender
     * @param msg       message bytes
     * @param respond   corresponding response processor
     * @throws Exception    any problem
     */
    void
    accept(SocketAddress from, ByteArray msg,
           Do<ByteArray,?> respond) throws Exception;
}
