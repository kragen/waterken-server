// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.crypto.aes;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.waterken.crypto.Encryptor;

/**
 * AES encryption
 */
public class
AES implements Encryptor {
    
    public byte[]
    run(final byte[] key, final byte[] plaintext) throws Exception {
        final Cipher ecb = Cipher.getInstance("AES/ECB/NoPadding");
        ecb.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        final byte[] cyphertext = new byte[128 / Byte.SIZE];
        ecb.doFinal(plaintext, 0, plaintext.length, cyphertext);
        return cyphertext;
    }
}
