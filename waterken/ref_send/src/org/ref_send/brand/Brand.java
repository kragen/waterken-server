// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.brand;

import java.io.Serializable;

import org.joe_e.Powerless;

/**
 * An opaque, globally unique identifier.
 * @param <T> purpose of this brand
 */
public class
Brand<T> implements Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    protected Brand() {}
    
    /**
     * Constructs an instance.
     * @param <T> purpose of this brand
     */
    static public <T> Brand<T>
    make() { return new Brand<T>(); }
    
    /**
     * Requires that this brand is the same as another.
     * @param expected  expected brand
     * @param actual    brand to compare against  
     * @throws WrongBrand   <code>actual</code> not <code>expected</code>
     */
    static public void
    require(final Brand<?> expected, final Brand<?> actual) throws WrongBrand {
        if (!expected.equals(actual)) { throw new WrongBrand(expected,actual); }
    }
}
