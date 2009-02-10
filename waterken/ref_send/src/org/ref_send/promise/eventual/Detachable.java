// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import org.ref_send.promise.Fulfilled;

/**
 * A promise whose referent should be left on disk until needed. 
 */
/* package */ final class
Detachable<T> extends Fulfilled<T> {
    static private final long serialVersionUID = 1L;

    /**
     * referent
     */
    private final T value;

    /**
     * Construct an instance.
     * @param value {@link #cast value}
     */
    protected
    Detachable(final T value) {
        this.value = value;
    }

    // org.ref_send.promise.Volatile interface

    /**
     * Gets the fulfilled value.
     */
    public T
    cast() { return value; }
}
