// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

/**
 * A fulfilled promise that should not use persistent object faulting.
 */
public final class
Inline<T> extends Fulfilled<T> {
    static private final long serialVersionUID = 1L;

    Inline(final T value) {
        super(value);
    }
}
