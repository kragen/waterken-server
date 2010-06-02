// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.brand;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals a {@link Brand} mismatch.
 */
public class
WrongBrand extends ClassCastException implements Powerless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * expected brand
     */
    public final Brand<?> expected;
    
    /**
     * actual brand
     */
    public final Brand<?> actual;
    
    /**
     * Constructs an instance.
     * @param expected  {@link #expected}
     * @param actual    {@link #actual}
     */
    public @deserializer
    WrongBrand(@name("expected") final Brand<?> expected,
               @name("actual") final Brand<?> actual) {
        this.expected = expected;
        this.actual = actual;
    }
}
