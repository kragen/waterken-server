// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import org.joe_e.array.ByteArray;

/**
 * Public key fingerprint algorithm. 
 */
public interface
Digest {

    /**
     * Calculates a key fingerprint.
     * @param key   key to fingerprint
     * @return fingerprint
     */
    ByteArray
    run(ByteArray key);
}
