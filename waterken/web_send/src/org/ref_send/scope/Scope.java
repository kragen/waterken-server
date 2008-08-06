// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.scope;

import java.io.Serializable;

import org.joe_e.Selfless;
import org.joe_e.array.ConstArray;

/**
 * A [ name =&gt; value ] record.
 */
public final class
Scope implements Selfless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * metadata
     */
    public final Layout meta;

    /**
     * each corresponding value
     */
    public final ConstArray<?> values;
    
    protected
    Scope(final Layout meta, final ConstArray<?> values) {
        if (meta.names.length()!=values.length()){throw new RuntimeException();}
        
        this.meta = meta;
        this.values = values;
    }
    
    // java.lang.Object interface
    
    /**
     * Is the given object the same?
     * @param o compared to object
     * @return <code>true</code> if the same, else <code>false</code>
     */
    public boolean
    equals(final Object o) {
        return null != o && Scope.class == o.getClass() &&
               values.equals(((Scope)o).values) &&
               meta.equals(((Scope)o).meta);
    }
    
    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0x4EF5C09E + meta.hashCode() + values.hashCode(); }
    
    // org.ref_send.scope.Scope interface
    
    /**
     * Gets the named value.
     * @param <T>   expected type of value
     * @param name  member name
     * @return member value, or <code>null</code> if unspecified
     */
    public @SuppressWarnings("unchecked") <T> T
    get(final String name) {
        final int i = meta.find(name);
        return -1 != i ? (T)values.get(i) : null;
    }
}
