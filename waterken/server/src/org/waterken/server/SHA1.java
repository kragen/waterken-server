// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.waterken.dns.editor.redirectory.Digest;

/**
 * SHA1 hash algorithm
 */
public class
SHA1 extends Struct implements Digest, Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    SHA1() {}
    
    // org.waterken.dns.editor.redirectory.Digest interface
    
    public ByteArray
    run(final ByteArray key) {
        final MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
        } catch (final NoSuchAlgorithmException e) {
            throw new AssertionError(); // should never happen
        }
        return ByteArray.array(sha1.digest(key.toByteArray()));
    }
}
