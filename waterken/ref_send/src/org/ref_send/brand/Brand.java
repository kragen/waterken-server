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
     * Is one object {@link Object#equals equal} to another?
     * @param <P>   compared object type
     * @param a object compared against
     * @param b object compared to 
     * @return {@code true} if {@code a} claims {@code b} is
     *         {@link Object#equals equal}, else {@code false}
     */
    static public <P> boolean
    equal(final P a, final P b) { return null != a ? a.equals(b) : null == b; }
    
    /**
     * Requires that one brand is the same as another.
     * @param <T> purpose of this brand
     * @param expected  brand compared against
     * @param actual    brand compared to 
     * @throws WrongBrand   {@code actual} is not {@code expected}
     */
    static public <T> void
    require(final Brand<T> expected, final Brand<T> actual) throws WrongBrand {
        if (!equal(expected, actual)) { throw new WrongBrand(expected,actual); }
    }
}
