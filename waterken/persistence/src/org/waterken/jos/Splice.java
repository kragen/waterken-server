// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.waterken.model.Heap;

/**
 * An object graph splice.
 */
final class
Splice implements Wrapper, Powerless, Selfless  {
    static private final long serialVersionUID = 1L;

    /**
     * object address
     */
    private final long address;

    Splice(final long address) {
        this.address = address;
    }

    // java.lang.Object interface

    public boolean
    equals(final Object x) {
        return x instanceof Splice && address == ((Splice)x).address;
    }

    public int
    hashCode() { return (int)(address >>> 32) + (int)address; }
    
    // org.waterken.jos.Wrapper interface

    public Object
    peel(final Heap loader) { return loader.reference(address); }
}
