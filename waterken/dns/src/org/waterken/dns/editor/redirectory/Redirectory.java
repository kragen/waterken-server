// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.Promise;
import org.waterken.dns.editor.DomainMaster;
import org.web_send.graph.Collision;

/**
 * A {@link Registrar} implementation.
 */
public interface
Redirectory {

    /**
     * Registers a public key.
     * @param strength  number of hash bits to use: MUST be at least 80
     * @param key       DER encoded ASN.1 SubjectPublicKeyInfo
     * @return administrator permissions for the domain
     * @throws Collision    <code>key</code> has already been registered
     */
    Promise<DomainMaster>
    register(int strength, ByteArray key) throws Collision;
}
