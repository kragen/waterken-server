// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.Serializable;

import org.joe_e.Selfless;
import org.ref_send.promise.Promise;
import org.waterken.db.Root;

/**
 * An object faulting promise.
 */
/* package */ final class
Faulting implements Promise<Object>, Selfless, Serializable  {
    static private final long serialVersionUID = 1L;

    private final Root root;
    private final String name;

    Faulting(final Root root, final String name) {
        if (null == root) { throw new NullPointerException(); }
        if (null == name) { throw new NullPointerException(); }
        
        this.root = root;
        this.name = "" + name;   // ensure we're not using the cache key string
    }

    // java.lang.Object interface

    public boolean
    equals(final Object x) {
        return x instanceof Faulting &&
               name.equals(((Faulting)x).name) &&
               root.equals(((Faulting)x).root);
    }
    
    public int
    hashCode() { return 0xFA017126; }

    // org.ref_send.promise.Promise interface

    public Object
    call() { return root.fetch(null, name); }
}
