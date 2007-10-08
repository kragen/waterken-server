// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import org.ref_send.promise.Promise;
import org.waterken.dns.editor.DomainMaster;
import org.web_send.graph.Collision;

/**
 * A fingerprint registry.
 */
public interface
Redirectory {

    /**
     * Registers a public key.
     * @param fingerprint   domain label
     * @return administrator permissions for the domain
     * @throws Collision    <code>fingerprint</code> already registered
     */
    Promise<DomainMaster>
    register(String fingerprint) throws Collision;
}
