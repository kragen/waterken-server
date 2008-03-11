// Copyright 2006-08 Regents of the University of California.  May be used 
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
     * Constructs an array of <code>float</code>s.
     * @param floats each element
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
     * as {@link java.util.Arrays#hashCode(Object[])} called on a Java array
     * containing the same elements.
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
     * Creates a <code>Float</code> for a specified <code>float</code>.
     * @param i position of the element to return
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
     * Creates a <code>FloatArray<code> with an appended <code>Float</code>.
     * @param newFloat   the element to append
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
     * @param i position of the element to return
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
     * Creates a <code>FloatArray</code> with an appended <code>float</code>.
     * @param newFloat   the element to append
     */
    public FloatArray with(final float newFloat) {
        final float[] newFloats = new float[floats.length + 1];
        System.arraycopy(floats, 0, newFloats, 0, floats.length);
        newFloats[floats.length] = newFloat;
        return new FloatArray(newFloats);
    }

    /**
     * Return a new <code>FloatArray</code> that contains the same elements
     * as this one excluding the element at a specified index
     * @param i the index of the element to exclude
     * @return  the new array
     */
    public FloatArray without(final int i) {
        final float[] newArr = new float[floats.length - 1];
        System.arraycopy(floats, 0, newArr, 0, i);
        System.arraycopy(floats, i + 1, newArr, i, newArr.length - i);
        return new FloatArray(newArr);
    }
    
    /**
     * A {@link FloatArray} factory.
     */
    static public final class Builder extends PowerlessArray.Builder<Float> {
        private float[] buffer;
        private int size;

        /**
         * Construct an instance with the default internal array length.
         */
        public Builder() {
            this(0);
        }
        
        /**
         * Construct an instance.
         * @param estimate  estimated array length
         */
        public Builder(int estimate) {
            buffer = new float[estimate > 0 ? estimate : 32];
            size = 0;
        }

        // ArrayBuilder<Float> interface
        /**
         * Append a <code>Float</code>
         * @param newFloat the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(Float newFloat) {
            append ((float) newFloat);
        }

        /**
         * Append an array of <code>Float</code>s
         * @param newFloats the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final Float[] newFloats) {
            append(newFloats, 0, newFloats.length);
        }      

        /**
         * Append a range of elements from an array of <code>Float</code>s
         * @param newFloats the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final Float[] newFloats, 
                          final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newFloats.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new float[newLength], 0,
                                 size);
            }
            
            for (int i = 0; i < len; ++i) {
                buffer[size + i] = newFloats[off + i];
            }           
            size = newSize;
        }
        
        /**
         * Create a snapshot of the current content.
         * @return a <code>FloatArray</code> containing the elements so far
         */
        public FloatArray snapshot() {
            final float[] arr;
            if (size == buffer.length) {
                arr = buffer;
            } else {
                arr = new float[size];
                System.arraycopy(buffer, 0, arr, 0, size);
            }
            return new FloatArray(arr);
        }
        
        /*
         * Convenience (more efficient) methods with float
         */       
        /**
         * Append a <code>float</code>
         * @param newFloat the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final float newFloat) {
            if (size == buffer.length) {
                System.arraycopy(buffer, 0, buffer = new float[2 * size], 0,
                                 size);
            }
            buffer[size++] = newFloat;
        }

        /**
         * Append an array of <code>float</code>s
         * @param newFloats the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final float[] newFloats) {
            append(newFloats, 0, newFloats.length);
        }      

        /**
         * Append a range of elements from an array of <code>float</code>s
         * @param newFloats the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final float[] newFloats, final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newFloats.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new float[newLength], 0,
                                 size);
            }
            System.arraycopy(newFloats, off, buffer, size, len);
            size = newSize;
        }
    }
    
    /* If one only invokes static methods statically, this is sound, since
     * FloatArray extends PowerlessArray<Float> and thus this method is
     * only required to return something of a type covariant with
     * PowerlessArray.Builder<Float>.  Unfortunately, this is not completely
     * sound because it is possible to invoke static methods on instances, e.g.
     * ConstArray.Builder<String> = (ConstArray (FloatArray.array())).builder(),
     * allowing for heap pollution without an unchecked cast warning.
     * 
     * The only solution to this would be to completely de-genericize these
     * methods.
     */

    /**
     * Get a <code>FloatArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @return a new builder instance, with the default internal array length
     */
    @SuppressWarnings("unchecked")
    public static Builder builder() {
        return new Builder(0);
    }

    /**
     * Get a <code>FloatArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @param estimate  estimated array length  
     * @return a new builder instance
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final int estimate) {
        return new Builder(estimate);
    }
}
