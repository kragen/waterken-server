// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.scope;

import java.io.Serializable;

import org.joe_e.Selfless;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;

/**
 * A [ name =&gt; value ] mapping.
 */
public final class
Scope implements Selfless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * each member name
     * <p>
     * A member name MUST NOT be either <code>null</code> or <code>"@"</code>.
     * </p>
     */
    public final PowerlessArray<String> names;

    /**
     * each corresponding value
     */
    public final ConstArray<?> values;
    
    /**
     * Constructs an instance.
     * @param names     {@link #names}
     * @param values    {@link #values}
     */
    public
    Scope(final PowerlessArray<String> names, final ConstArray<?> values) {
        if (names.length() != values.length()) {throw new RuntimeException();}
        for (final String name : names) {
            if (name.equals("@")) { throw new RuntimeException(); }
        }
        
        this.names = names;
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
               names.equals(((Scope)o).names);
    }
    
    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0x4EF5C09E + names.hashCode() + values.hashCode(); }
    
    // org.ref_send.scope.Scope interface
    
    /**
     * Gets the named value.
     * @param <T>   expected type of value
     * @param name  member name
     * @return member value, or <code>null</code> if unspecified
     */
    public @SuppressWarnings("unchecked") <T> T
    get(final String name) {
        for (int i = names.length(); 0 != i--;) {
            if (names.get(i).equals(name)) { return (T)values.get(i); }
        }
        return null;
    }
}
