// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;


/**
 * A fulfilled promise that should not use persistent object faulting.
 */
final class
Inline<T> extends Fulfilled<T> {
    static private final long serialVersionUID = 1L;

    /**
     * referent
     */
    public final T value;

    /**
     * Construct an instance.
     * @param value {@link #value value}
     */
    protected
    Inline(final T value) {
        this.value = value;
    }

    // org.ref_send.promise.Volatile interface

    /**
     * Gets the {@link #value}.
     */
    public T
    cast() { return value; }
}
