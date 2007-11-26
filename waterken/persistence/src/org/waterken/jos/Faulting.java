// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import org.ref_send.promise.Fulfilled;
import org.waterken.model.Root;

/**
 * An object faulting promise.
 * @param <T> referent type
 */
final class
Faulting<T> extends Fulfilled<T>  {
    static private final long serialVersionUID = 1L;

    final Root root;
    final String name;

    Faulting(final Root root, final String name) {
        super(null);

        if (null == root) { throw new NullPointerException(); }
        if (null == name) { throw new NullPointerException(); }
        
        this.root = root;
        this.name = name;
    }

    // java.lang.Object interface

    public boolean
    equals(final Object x) {
        return x instanceof Faulting
            ? name.equals(((Faulting)x).name) &&
              root.equals(((Faulting)x).root)
            : super.equals(x);
    }

    // org.ref_send.promise.Volatile interface

    @SuppressWarnings("unchecked") public T
    cast() { return (T)root.fetch(null, name); }
}
