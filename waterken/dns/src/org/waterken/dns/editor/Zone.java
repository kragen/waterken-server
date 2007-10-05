// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.ref_send.promise.Promise;
import org.web_send.graph.Collision;

/**
 * An {@link DomainMaster} factory.
 */
public interface
Zone {

    /**
     * Creates a new domain.
     * @param hostname  domain's hostname
     * @return administrator permissions for the domain
     * @throws Collision    <code>hostname</code> has already been claimed
     */
    Promise<DomainMaster>
    claim(String hostname) throws Collision;
}
