// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.joe_e.Powerless;
import org.joe_e.Struct;

/**
 * Hides the mutable state inside a {@link BigDecimal}.
 */
/* package */ final class
BigDecimalWrapper extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1;

    private final BigInteger unscaled;
    private final int scale;
    
    BigDecimalWrapper(final BigDecimal value) {
        unscaled = value.unscaledValue();
        scale = value.scale();
    }
    
    private Object
    readResolve() { return new BigDecimal(unscaled, scale); }
}
