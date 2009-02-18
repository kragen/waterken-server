// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Vat;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;
import org.waterken.var.Guard;

/**
 * A {@link Resource} {@link Menu} factory.
 */
public interface
Registrar {
    
    /**
     * Gets the restrictions on the hostname.
     */
    Guard<String> getHostnameGuard();

    /**
     * Registers a hostname.
     * @param hostname  hostname
     * @return administrator permissions for the host
     * @throws RuntimeException <code>hostname</code> already claimed
     */
    Promise<Vat<Menu<ByteArray>>> claim(String hostname)throws RuntimeException;
}
