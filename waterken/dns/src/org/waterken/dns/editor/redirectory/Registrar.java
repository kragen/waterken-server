// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.Promise;
import org.waterken.dns.Resource;
import org.waterken.dns.editor.DomainMaster;
import org.web_send.graph.Collision;

/**
 * A public key registry.
 */
public interface
Registrar {

    /**
     * Registers a public key.
     * @param key   encoded ASN.1 SubjectPublicKeyInfo
     * @return administrator permissions for the domain
     * @throws Collision    <code>key</code> has already been registered
     */
    Promise<DomainMaster<Promise<Resource>>>
    register(ByteArray key) throws Collision;
}
