// Copyright 2004-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

/**
 * A site locator.
 */
public interface
Locator {

    /**
     * Produce the canonicalized version of a URI authority.
     * @param authority authority to canonicalize
     * @return canonicalized <code>authority</code>
     */
    String
    canonicalize(String authority);
    
    /**
     * Opens a socket to the identified host.
     * @param authority     URL authority identifying the target host
     * @param mostRecent    most recent location, or <code>null</code>
     * @return open socket to the host
     */
    Socket
    locate(String authority, SocketAddress mostRecent) throws IOException;
}
