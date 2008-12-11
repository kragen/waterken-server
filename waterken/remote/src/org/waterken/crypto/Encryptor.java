// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.crypto;

/**
 * 
 */
public interface
Encryptor {

    byte[]
    run(final byte[] key, final byte[] plaintext) throws Exception;
}
