// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Selfless;

/**
 * A direct promise for a referent.
 */
/* package */ final class
Inline<T> implements Promise<T>, Selfless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * referent
     */
    private final T referent;

    /**
     * Construct an instance.
     * @param referent  {@link #referent}
     */
    protected
    Inline(final T referent) {
        this.referent = referent;
    }
    
    // java.lang.Object interface
    
    public boolean
    equals(final Object x) {
        return x instanceof Inline && (null == referent ?
                null == ((Inline<?>)x).referent :
            referent.equals(((Inline<?>)x).referent));
    }
    
    public int
    hashCode() { return 0xFA571A2E; }

    // org.ref_send.promise.Promise interface

    public T
    call() { return referent; }
}
