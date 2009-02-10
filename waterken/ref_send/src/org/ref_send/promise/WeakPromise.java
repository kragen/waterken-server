// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import org.joe_e.Equatable;
import org.joe_e.Struct;

/**
 * A promise that may allow the referent to be garbage collected.
 */
/* package */ final class
WeakPromise<T extends Equatable> extends Struct implements Volatile<T> {

    /**
     * referent
     */
    private final T value;

    /**
     * Construct an instance.
     * @param value {@link #cast value}
     */
    protected
    WeakPromise(final T value) {
        this.value = value;
    }

    // org.ref_send.promise.Volatile interface

    /**
     * Gets the fulfilled value.
     */
    public T
    cast() { return value; }
}
