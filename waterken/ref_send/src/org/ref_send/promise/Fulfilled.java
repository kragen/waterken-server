// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

/**
 * A fulfilled promise.
 */
/* package */ final class
Fulfilled<T> implements Promise<T>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Is the referent only weakly referred to?
     */
    private final boolean isWeak;
    
    /**
     * promise for referent
     */
    private       Promise<T> state;

    /**
     * Constructs an instance.
     * @param isWeak    {@link #isWeak}
     * @param referent  referent
     */
    /* package */
    Fulfilled(final boolean isWeak, final T referent) {
        this.isWeak = isWeak;
        this.state = new Inline<T>(referent);
    }
    
    protected Promise<T>
    getState() { return state; }
    
    // java.lang.Object interface

    /**
     * Is the given object a fulfilled promise for the same referent?
     * @param x compared to object
     * @return <code>true</code> if equivalent, else <code>false</code>
     */
    public boolean
    equals(final Object x) {
        try {
            return x instanceof Fulfilled &&
                isWeak == ((Fulfilled<?>)x).isWeak &&
                (state.getClass() == ((Fulfilled<?>)x).state.getClass()
                    ? state.equals(((Fulfilled<?>)x).state)
                 : same(call(), ((Fulfilled<?>)x).call()));
        } catch (final Exception e) { return false; }
    }

    static private boolean
    same(final Object a, final Object b) {
        return null != a ? a.equals(b) : null == b;
    }

    /**
     * Calculates the hash code.
     */
    public final int
    hashCode() { return 0xD7ACAB1E; }

    // org.ref_send.promise.Promise interface

    /**
     * Gets the current referent.
     */
    public T
    call() throws Exception { return state.call(); }
}
