// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Selfless;

/**
 * A promise that alleges to be born in the fulfilled state.
 * @param <T> referent type
 */
public abstract class
Fulfilled<T> implements Promise<T>, Selfless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Construct an instance.
     */
    protected
    Fulfilled() {}

    /**
     * Is the given object the same?
     * @param x compared to object
     * @return <code>true</code> if the same, else <code>false</code>
     */
    public boolean
    equals(final Object x) {
        try {
            return x instanceof Fulfilled &&
                   same(cast(), ((Fulfilled<?>)x).cast());
        } catch (final Exception e) {
            return false;
        }
    }

    static private boolean
    same(final Object a, final Object b) {
        return null != a ? a.equals(b) : null == b;
    }

    /**
     * Calculates the hash code.
     */
    public final int
    hashCode() { return 0xF0F111ED; }
}
