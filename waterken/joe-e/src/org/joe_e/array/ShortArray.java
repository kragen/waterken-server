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
 * An immutable array of <code>short</code>.
 */
public final class ShortArray extends PowerlessArray<Short> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient short[] shorts;

    ShortArray(short... shorts) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.shorts = shorts;
    }
    
    /**
     * Constructs an array of <code>short</code>s.
     * @param shorts each element
     */
    static public ShortArray array(final short... shorts) {
        return new ShortArray(shorts.clone());
    }
    
    /*
     * The following are necessary because otherwise calls with <=4 arguments
     * are resolved to the superclass PowerlessArray
     */
    
    /**
     * Construct an empty <code>ShortArray</code>
     */
    @SuppressWarnings("unchecked")  // the warning here seems completely bogus
    static public ShortArray array() {
        return new ShortArray(new short[]{});
    }

    /**
     * Construct a <code>ShortArray</code> with one element.
     * @param value    the value
     */    
    static public ShortArray array(short value) {
        return new ShortArray(new short[]{value});
    }
    
    /**
     * Construct a <code>ShortArray</code> with two elements.
     * @param value1    the first value
     * @param value2    the second value
     */     
    static public ShortArray array(short value1, short value2) {
        return new ShortArray(new short[]{value1, value2});
    }
    
    /**
     * Construct a <code>ShortArray</code> with three elements.
     * @param value1    the first value
     * @param value2    the second value
     * @param value3    the third value
     */     
    static public ShortArray array(short value1, short value2, short value3) {
        return new ShortArray(new short[]{value1, value2, value3});
    }
    
    /**
     * Construct a <code>ShortArray</code> with four elements.
     * @param value1    the first value
     * @param value2    the second value
     * @param value3    the third value
     * @param value4    the fourth value
     */    
    static public ShortArray array(short value1, short value2, short value3,
                                  short value4) {
        return new ShortArray(new short[]{value1, value2, value3, value4});
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

    private void readObject(final ObjectInputStream in) throws 
                                        IOException, ClassNotFoundException {
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
        } else if (other instanceof ConstArray<?>) {
            // Other array does not have contents in shortArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray<?> otherArray = (ConstArray<?>)other;
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
     * as {@link java.util.Arrays#hashCode(Object[])} called on a Java array
     * containing the same elements.
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
     * Creates a <code>Short</code> for a specified <code>short</code>.
     * @param i position of the element to return
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
            final Class<?> t = prototype.getClass().getComponentType(); 
            prototype = (T[])Array.newInstance(t, len);
        }
        
        for (int i = 0; i < len; ++i) {
            prototype[i] = (T) (Short) shorts[i];
        }
        return prototype;
    }
    
    /**
     * Creates a <code>ShortArray<code> with an appended <code>Short</code>.
     * @param newShort   the element to append
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
     * @param i position of the element to return
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
     * Creates a <code>ShortArray</code> with an appended <code>short</code>.
     * @param newShort   the element to append
     */
    public ShortArray with(final short newShort) {
        final short[] newShorts = new short[shorts.length + 1];
        System.arraycopy(shorts, 0, newShorts, 0, shorts.length);
        newShorts[shorts.length] = newShort;
        return new ShortArray(newShorts);
    }

    /**
     * Return a new <code>ShortArray</code> that contains the same elements
     * as this one excluding the element at a specified index
     * @param i the index of the element to exclude
     * @return  the new array
     */
    public ShortArray without(final int i) {
        final short[] newArr = new short[shorts.length - 1];
        System.arraycopy(shorts, 0, newArr, 0, i);
        System.arraycopy(shorts, i + 1, newArr, i, newArr.length - i);
        return new ShortArray(newArr);
    }
    
    /**
     * A {@link ShortArray} factory.
     */
    public static final class Builder extends 
                                        PowerlessArray.Builder<Short> {
        private short[] shortBuffer;

        /**
         * Construct an instance with the default internal array length.
         */
        Builder() {
            this(0);
        }
        
        /**
         * Construct an instance.
         * @param estimate  estimated array length
         */
        Builder(int estimate) {
            shortBuffer = new short[estimate > 0 ? estimate : 32];
            size = 0;
        }

        // ArrayBuilder<Short> interface
        /**
         * Append a <code>Short</code>
         * @param newShort the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(Short newShort) {
            append ((short) newShort);
        }

        /**
         * Append an array of <code>Short</code>s
         * @param newShorts the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final Short[] newShorts) {
            append(newShorts, 0, newShorts.length);
        }      

        /**
         * Append a range of elements from an array of <code>Short</code>s
         * @param newShorts the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final Short[] newShorts, 
                          final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newShorts.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > shortBuffer.length) {
                int newLength = Math.max(newSize, 2 * shortBuffer.length);
                System.arraycopy(shortBuffer, 0, 
                                 shortBuffer = new short[newLength], 0, size);
            }
            
            for (int i = 0; i < len; ++i) {
                shortBuffer[size + i] = newShorts[off + i];
            }           
            size = newSize;
        }
        
        /**
         * Create a snapshot of the current content.
         * @return a <code>ShortArray</code> containing the elements so far
         */
        public ShortArray snapshot() {
            final short[] arr;
            if (size == shortBuffer.length) {
                arr = shortBuffer;
            } else {
                arr = new short[size];
                System.arraycopy(shortBuffer, 0, arr, 0, size);
            }
            return new ShortArray(arr);
        }
        
        /*
         * Convenience (more efficient) methods with short
         */       
        /**
         * Append a <code>short</code>
         * @param newShort the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final short newShort) {
            if (size == shortBuffer.length) {
                System.arraycopy(shortBuffer, 0,
                                 shortBuffer = new short[2 * size], 0, size);
            }
            shortBuffer[size++] = newShort;
        }

        /**
         * Append an array of <code>short</code>s
         * @param newShorts the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final short[] newShorts) {
            append(newShorts, 0, newShorts.length);
        }      

        /**
         * Append a range of elements from an array of <code>short</code>s
         * @param newShorts the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final short[] newShorts, final int off, 
                           final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newShorts.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > shortBuffer.length) {
                int newLength = Math.max(newSize, 2 * shortBuffer.length);
                System.arraycopy(shortBuffer, 0, 
                                 shortBuffer = new short[newLength], 0, size);
            }
            System.arraycopy(newShorts, off, shortBuffer, size, len);
            size = newSize;
        }
    }
    
    /* If one only invokes static methods statically, this is sound, since
     * ByteArray extends PowerlessArray<Byte> and thus this method is
     * only required to return something of a type covariant with
     * PowerlessArray.Builder<Byte>.  Unfortunately, this is not completely
     * sound because it is possible to invoke static methods on instances, e.g.
     * ConstArray.Builder<String> = (ConstArray (ShortArray.array())).builder()
     * Invocations of append() can then throw ClassCastExceptions.
     * 
     * I can't see a way to avoid this other than to de-genericize everything.
     */

    /**
     * Get a <code>ShortArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @return a new builder instance, with the default internal array length
     */
    @SuppressWarnings("unchecked")
    public static Builder builder() {
        return new Builder(0);
    }

    /**
     * Get a <code>ShortArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @param estimate  estimated array length  
     * @return a new builder instance
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final int estimate) {
        return new Builder(estimate);
    }
}
