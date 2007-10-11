// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import org.ref_send.deserializer;

/**
 * Signals a negative infinity.
 */
public class
NegativeInfinity extends Infinity {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    NegativeInfinity() {}
}
