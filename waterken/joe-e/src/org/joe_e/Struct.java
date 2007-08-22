// Copyright 2006 Regents of the University of California.  May be used
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;

/**
 * This abstract class contains implementations of the equals() and hashCode()
 * methods that satisfy the Selfless interface.  The provided equals() method
 * computes equality of all fields using reflection.
 */
public abstract class Struct implements Selfless {

    protected Struct() {}
    
    /**
     * Tests for equality with another object.  An obect is equal to this one
     * if it is of identical type and each field is equal for the two objects.
     * The objects' fields are equal if both are null, or if their values
     * return true for <code>equals()</code>.  This implementation uses
     * reflection to work for any subclass of <code>Struct</code>.
     * @param other candidate object
     * @return true if it is equal to this object
     */
    public final boolean equals(final Object other) {
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        
        // traverse class hierarchy, finding declared fields.  This is
        // necessary since getFields() only returns public fields.
        for (Class i = getClass(); i != Struct.class; i = i.getSuperclass()) {
            final Field[] fields = i.getDeclaredFields();
            AccessibleObject.setAccessible(fields, true);
            Arrays.sort(fields, 
                new Comparator<Field>() {
                    public int compare(final Field a, final Field b) {
                         return a.getName().compareTo(b.getName());
                    }
                });
            for (final Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    try {
                        final Object a = f.get(this);
                        final Object b = f.get(other);                          
                        if (a == null ? b != null : !a.equals(b)) {
                            return false;
                        }
                    } catch (final IllegalAccessException e) {
                        // Should never happen.
                        throw new IllegalAccessError();
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Calculates the hash code.
     * 
     * This method will satisfy the contract of the hashCode method for any
     * subclass of <code>Struct</code>.  (two structs that are 
     * <code>equal()</code> structs will always have the same hashCode).  The 
     * precise return value of this method is unspecified, and may change in
     * future releases.
     * @return a hash value
     */
    public final int hashCode() { 
        return getClass().getName().hashCode();
    }
}
