// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.test.base32;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.waterken.base32.Base32;
import org.waterken.io.Stream;

/**
 * Tests the {@link Base32} class.
 */
final class
Main {

    private
    Main() {}

    /**
     * Encodes bytes read from <code>stdin</code>.
     */
    static public void
    main(final String[] args) throws Exception {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Stream.copy(System.in, buffer);
        final byte[] bytes = buffer.toByteArray();
        final String encoded = Base32.encode(bytes);
        final byte[] decoded = Base32.decode(encoded);
        if (!Arrays.equals(bytes, decoded)) { throw new Exception(); }
        System.out.println(encoded);
    }
}
