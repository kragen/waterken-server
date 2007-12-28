// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.joe_e.Powerless;
import org.waterken.model.Root;

/**
 * Hides the mutable state inside a {@link BigDecimal}.
 */
final class
BigDecimalWrapper implements Wrapper, Powerless {
    static private final long serialVersionUID = 1;

    private transient BigDecimal value;
    
    BigDecimalWrapper(final BigDecimal value) {
        this.value = value;
    }
    
    // java.io.Serializable interface
    
    private void
    writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeObject(value.unscaledValue());
        out.writeInt(value.scale());
    }

    private void
    readObject(final ObjectInputStream in) throws IOException,
                                                  ClassNotFoundException {
        in.defaultReadObject();

        final BigInteger unscaledValue = (BigInteger)in.readObject();
        final int scale = in.readInt();
        value = new BigDecimal(unscaledValue, scale);
    }

    // org.waterken.jos.Wrapper interface
    
    public BigDecimal
    peel(final Root root) { return value; }
}
