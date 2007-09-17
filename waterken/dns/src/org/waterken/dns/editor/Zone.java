// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.ref_send.promise.Promise;
import org.waterken.dns.Resource;

/**
 * An {@link Administrator} factory.
 */
public interface
Zone {

    /**
     * Creates a new domain.
     * @param hostname  domain's hostname
     * @return administrator permissions for the domain
     */
    Promise<Administrator<Promise<Resource>>>
    claim(String hostname);
}
