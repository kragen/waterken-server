// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.mux;

import org.waterken.db.Database;
import org.waterken.http.Server;

/**
 * A remoting protocol.
 */
public interface
Remoting<S> {
    
    /**
     * Wrap a network interface around a vat.
     * @param bootstrap bootstrap server
     * @param scheme    URI scheme
     * @param vat       vat
     * @return network interface
     */
    Server
    remote(Server bootstrap, String scheme, Database<S> vat);
}
