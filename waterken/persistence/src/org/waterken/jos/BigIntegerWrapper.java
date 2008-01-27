// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;

import org.joe_e.Powerless;
import org.waterken.model.Root;

/**
 * Hides the mutable state inside a {@link BigInteger}.
 */
final class
BigIntegerWrapper implements Wrapper, Powerless {
    static private final long serialVersionUID = 1;

    private transient BigInteger value;
    
    BigIntegerWrapper(final BigInteger value) {
        this.value = value;
    }
    
    // java.io.Serializable interface
    
    private void
    writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        final byte[] bytes = value.toByteArray();
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private void
    readObject(final ObjectInputStream in) throws IOException,
                                                  ClassNotFoundException {
        in.defaultReadObject();

        final byte[] bytes = new byte[in.readInt()];
        in.readFully(bytes);
        value = new BigInteger(bytes);
    }

    // org.waterken.jos.Wrapper interface
    
    public BigInteger
    peel(final Root root) { return value; }
}
