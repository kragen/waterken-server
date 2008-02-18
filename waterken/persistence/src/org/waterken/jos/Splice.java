// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.waterken.vat.Root;

/**
 * An object graph splice.
 */
final class
Splice implements Wrapper, Powerless, Selfless  {
    static private final long serialVersionUID = 1L;

    /**
     * name to fetch
     */
    private final String name;

    Splice(final String name) {
        if (null == name) { throw new NullPointerException(); }
        
        this.name = name;
    }

    // java.lang.Object interface

    public boolean
    equals(final Object x) {
        return x instanceof Splice && name.equals(((Splice)x).name);
    }

    public int
    hashCode() { return 0x591CE4EF + name.hashCode(); }
    
    // org.waterken.jos.Wrapper interface

    public Object
    peel(final Root root) { return root.fetch(null, name); }
}
