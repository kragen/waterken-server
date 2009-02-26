// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.Serializable;
import java.math.BigInteger;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;

/**
 * Hides the mutable state inside a {@link BigInteger}.
 */
/* package */ final class
BigIntegerWrapper extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1;

    private final ByteArray bytes;
    
    BigIntegerWrapper(final BigInteger value) {
        bytes = ByteArray.array(value.toByteArray());
    }
    
    private Object
    readResolve() { return new BigInteger(bytes.toByteArray());}
}
