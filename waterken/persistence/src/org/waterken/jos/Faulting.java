// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import org.ref_send.promise.Fulfilled;
import org.waterken.model.Heap;

/**
 * An object faulting promise.
 * @param <T> referent type
 */
final class
Faulting<T> extends Fulfilled<T>  {
    static private final long serialVersionUID = 1L;

    final Heap heap;
    final long address;

    Faulting(final Heap heap, final long address) {
        super(null);
        if (null == heap) { throw new NullPointerException(); }
        this.heap = heap;
        this.address = address;
    }

    // java.lang.Object interface

    public boolean
    equals(final Object x) {
        return x instanceof Faulting
            ? address == ((Faulting)x).address &&
              heap.equals(((Faulting)x).heap)
            : super.equals(x);
    }

    // org.ref_send.promise.Volatile interface

    @SuppressWarnings("unchecked") public T
    cast() { return (T)heap.reference(address); }
}
