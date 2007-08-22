// Copyright 2006 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e.array;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.lang.reflect.Array;


/**
 * An immutable array of <code>short</code>.
 */
public final class ShortArray extends PowerlessArray<Short> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient short[] shorts;

    private ShortArray(short... shorts) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.shorts = shorts;
    }
    
    /**
     * Constructs a {@link ShortArray}.
     * @param shorts each <code>short</code>
     */
    static public ShortArray array(final short... shorts) {
        return new ShortArray(shorts.clone());
    }
    
    // java.io.Serializable interface
    
    /*
     * Serialization hacks to prevent the contents from being serialized as a
     * mutable array.  This improves efficiency for projects that serialize
     * Joe-E objects using Java's serialization API by avoiding treatment of
     * immutable state as mutable.  These methods can otherwise be ignored.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeInt(shorts.length);
        for (short c : shorts) {
            out.writeShort(c);
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, 
    						                      ClassNotFoundException {
        in.defaultReadObject();

        final int length = in.readInt();
        shorts = new short[length];
        for (int i = 0; i < length; ++i) {
            shorts[i] = in.readShort();
        }
    }
    
    /*
     *  Methods that must be overriden, as the implementation in ConstArray
     *  would try to use arr, which is null.
     */
    
    // java.lang.Object interface
    
    /**
     * Test for equality with another object
     * @return true if the other object is a {@link ConstArray} with the same
     *         contents as this array
     */
    public boolean equals(final Object other) {
        if (other instanceof ShortArray) {
            // Simple case: just compare shortArr fields
            return Arrays.equals(shorts, ((ShortArray)other).shorts);
        } else if (other instanceof ConstArray) {
            // Other array does not have contents in shortArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray otherArray = (ConstArray)other;
            if (otherArray.length() != shorts.length) {
                return false;
            }            
            for (int i = 0; i < shorts.length; ++i) {
                final Object otherElement = otherArray.get(i);
                if (!(otherElement instanceof Short) ||
                    ((Short)otherElement).shortValue() != shorts[i]) {
                    return false;
                }
            }            
            return true;
        } else {
            // Only a ConstArray can be equal to a ShortArray
            return false;
        }
    }

    /**
     * Computes a digest of the array for hashing.  The hash code is the same
     * as <code>Arrays.hashCode()</code> called on a Java array containing the
     * same elements.
     * @return a hash code based on the contents of this array
     */
    public int hashCode() {
        // Because wrappers for primitive types return the same hashCode as 
        // their primitive values, a ShortArray has the same hashCode as a
        // ConstArray<Short> with the same contents.
        return Arrays.hashCode(shorts);
    }
    
    /**
     * Return a string representation of the array
     */    
    public String toString() { 
        return Arrays.toString(shorts);
    }
    
    // org.joe_e.ConstArray interface

    /**
     * Gets the length of the array.
     */
    public int length() { 
        return shorts.length;
    }
    
    /**
     * Creates a {@link Short} for a specified <code>short</code>.
     * @param i position of the <code>short</code> to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public Short get(int i) { 
        return shorts[i]; 
    }
    
    /**
     * Return a mutable copy of the array
     * @param prototype prototype of the array to copy into
     * @return an array containing the contents of this <code>ConstArray</code>
     *     of the same type as <code>prototype</code>
     * @throws ArrayStoreException if an element cannot be stored in the array
     */
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] prototype) {
        final int len = length();
        if (prototype.length < len) {
            final Class t = prototype.getClass().getComponentType(); 
            prototype = (T[])Array.newInstance(t, len);
        }
        
        for (int i = 0; i < len; ++i) {
            prototype[i] = (T) (Short) shorts[i];
        }
        return prototype;
    }
    
    /**
     * Creates a {@link ShortArray} with an appended {@link Short}.
     * @param newShort   the {@link Short} to append
     * @throws NullPointerException <code>newShort</code> is null
     */
    public ShortArray with(final Short newShort) {
        return with(newShort.shortValue());
    }
           
    /*
     * Convenience (more efficient) methods with short
     */
        
    /**
     * Gets the <code>short</code> at a specified position.
     * @param i position of the <code>short</code> to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public short getShort(final int i) { 
        return shorts[i]; 
    }

    /**
     * Creates a mutable copy of the <code>short</code> array
     */
    public short[] toShortArray() {
        return shorts.clone(); 
    }
    
    /** 
     * Creates a {@link ShortArray} with an appended <code>short</code>.
     * @param newShort   the <code>short</code> to append
     */
    public ShortArray with(final short newShort) {
        final short[] newShorts = new short[shorts.length + 1];
        System.arraycopy(shorts, 0, newShorts, 0, shorts.length);
        newShorts[shorts.length] = newShort;
        return new ShortArray(newShorts);
    }
}
