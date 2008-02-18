// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import org.ref_send.promise.eventual.Deferred;
import org.waterken.http.Server;
import org.waterken.vat.Model;
import org.waterken.vat.Root;

/**
 * A remoting protocol.
 */
public interface
Remoting {
    
    /**
     * {@link Root} name for the eventual operator
     */
    String _ = "._";
    
    /**
     * {@link Root} name for the outbound request {@link Server}
     */
    String client = ".client";
    
    /**
     * {@link Root} name for {@link Deferred} permission
     */
    String deferred = ".deferred";

    /**
     * {@link Root} name for the address of the local host
     */
    String here = ".here";

    /**
     * Wrap a network interface around a model.
     * @param bootstrap bootstrap server
     * @param scheme    URI scheme
     * @param model     model
     * @return network interface
     */
    Server
    remote(Server bootstrap, String scheme, Model model);
}
