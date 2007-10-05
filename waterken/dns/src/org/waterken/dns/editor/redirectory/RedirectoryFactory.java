// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import org.waterken.dns.editor.Zone;

/**
 * A {@link Redirectory} factory. 
 */
public interface
RedirectoryFactory {

    /**
     * Creates a zone.
     */
    Zone
    master();

    /**
     * Creates a public key registry.
     * @param digest    fingerprint algorithm
     * @param suffix    hostname suffix
     * @param zone      domain factory
     */
    Redirectory
    make(Digest digest, String suffix, Zone zone);
}
