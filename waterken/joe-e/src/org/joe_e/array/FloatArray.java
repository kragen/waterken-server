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
 * An immutable array of <code>float</code>.
 */
public final class FloatArray extends PowerlessArray<Float> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient float[] floats;

    private FloatArray(float... floats) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.floats = floats;
    }
    
    /**
     * Constructs a {@link FloatArray}.
     * @param floats each <code>float</code>
     */
    static public FloatArray array(final float... floats) {
        return new FloatArray(floats.clone());
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

        out.writeInt(floats.length);
        for (float c : floats) {
            out.writeFloat(c);
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, 
    						                      ClassNotFoundException {
        in.defaultReadObject();

        final int length = in.readInt();
        floats = new float[length];
        for (int i = 0; i < length; ++i) {
            floats[i] = in.readFloat();
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
        if (other instanceof FloatArray) {
            // Simple case: just compare floatArr fields
            return Arrays.equals(floats, ((FloatArray)other).floats);
        } else if (other instanceof ConstArray) {
            // Other array does not have contents in floatArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray otherArray = (ConstArray)other;
            if (otherArray.length() != floats.length) {
                return false;
            }            
            for (int i = 0; i < floats.length; ++i) {
                final Object otherElement = otherArray.get(i);
                if (!(otherElement instanceof Float) ||
                    ((Float)otherElement).floatValue() != floats[i]) {
                    return false;
                }
            }            
            return true;
        } else {
            // Only a ConstArray can be equal to a FloatArray
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
        // their primitive values, a FloatArray has the same hashCode as a
        // ConstArray<Float> with the same contents.
        return Arrays.hashCode(floats);
    }
    
    /**
     * Return a string representation of the array
     */    
    public String toString() { 
        return Arrays.toString(floats);
    }
    
    // org.joe_e.ConstArray interface

    /**
     * Gets the length of the array.
     */
    public int length() { 
        return floats.length;
    }
    
    /**
     * Creates a {@link Float} for a specified <code>float</code>.
     * @param i position of the <code>float</code> to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public Float get(int i) { 
        return floats[i]; 
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
            prototype[i] = (T) (Float) floats[i];
        }
        return prototype;
    }
    
    /**
     * Creates a {@link FloatArray} with an appended {@link Float}.
     * @param newFloat   the {@link Float} to append
     * @throws NullPointerException <code>newFloat</code> is null
     */
    public FloatArray with(final Float newFloat) {
        return with(newFloat.floatValue());
    }
           
    /*
     * Convenience (more efficient) methods with float
     */
        
    /**
     * Gets the <code>float</code> at a specified position.
     * @param i position of the <code>float</code> to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public float getFloat(final int i) { 
        return floats[i]; 
    }

    /**
     * Creates a mutable copy of the <code>float</code> array
     */
    public float[] toFloatArray() {
        return floats.clone(); 
    }
    
    /** 
     * Creates a {@link FloatArray} with an appended <code>float</code>.
     * @param newFloat   the <code>float</code> to append
     */
    public FloatArray with(final float newFloat) {
        final float[] newFloats = new float[floats.length + 1];
        System.arraycopy(floats, 0, newFloats, 0, floats.length);
        newFloats[floats.length] = newFloat;
        return new FloatArray(newFloats);
    }
}
