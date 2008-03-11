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
 * An immutable array of <code>long</code>.
 */
public final class LongArray extends PowerlessArray<Long> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient long[] longs;

    private LongArray(long... longs) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.longs = longs;
    }
    
    /**
     * Constructs an array of <code>long</code>s.
     * @param longs each element
     */
    static public LongArray array(final long... longs) {
        return new LongArray(longs.clone());
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

        out.writeInt(longs.length);
        for (long c : longs) {
            out.writeLong(c);
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, 
    						                      ClassNotFoundException {
        in.defaultReadObject();

        final int length = in.readInt();
        longs = new long[length];
        for (int i = 0; i < length; ++i) {
            longs[i] = in.readLong();
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
        if (other instanceof LongArray) {
            // Simple case: just compare longArr fields
            return Arrays.equals(longs, ((LongArray)other).longs);
        } else if (other instanceof ConstArray) {
            // Other array does not have contents in longArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray otherArray = (ConstArray)other;
            if (otherArray.length() != longs.length) {
                return false;
            }            
            for (int i = 0; i < longs.length; ++i) {
                final Object otherElement = otherArray.get(i);
                if (!(otherElement instanceof Long) ||
                    ((Long)otherElement).longValue() != longs[i]) {
                    return false;
                }
            }            
            return true;
        } else {
            // Only a ConstArray can be equal to a LongArray
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
        // their primitive values, a LongArray has the same hashCode as a
        // ConstArray<Long> with the same contents.
        return Arrays.hashCode(longs);
    }
    
    /**
     * Return a string representation of the array
     */    
    public String toString() { 
        return Arrays.toString(longs);
    }
    
    // org.joe_e.ConstArray interface

    /**
     * Gets the length of the array.
     */
    public int length() { 
        return longs.length;
    }
    
    /**
     * Creates a <code>Long</code> for a specified <code>long</code>.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public Long get(int i) { 
        return longs[i]; 
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
            prototype[i] = (T) (Long) longs[i];
        }
        return prototype;
    }
    
    /**
     * Creates a <code>LongArray<code> with an appended <code>Long</code>.
     * @param newLong   the element to append
     * @throws NullPointerException <code>newLong</code> is null
     */
    public LongArray with(final Long newLong) {
        return with(newLong.longValue());
    }
           
    /*
     * Convenience (more efficient) methods with long
     */
        
    /**
     * Gets the <code>long</code> at a specified position.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public long getLong(final int i) { 
        return longs[i]; 
    }

    /**
     * Creates a mutable copy of the <code>long</code> array
     */
    public long[] toLongArray() {
        return longs.clone(); 
    }
    
    /** 
     * Creates a <code>LongArray</code> with an appended <code>long</code>.
     * @param newLong   the element to append
     */
    public LongArray with(final long newLong) {
        final long[] newLongs = new long[longs.length + 1];
        System.arraycopy(longs, 0, newLongs, 0, longs.length);
        newLongs[longs.length] = newLong;
        return new LongArray(newLongs);
    }

    /**
     * Return a new <code>LongArray</code> that contains the same elements
     * as this one excluding the element at a specified index
     * @param i the index of the element to exclude
     * @return  the new array
     */
    public LongArray without(final int i) {
        final long[] newArr = new long[longs.length - 1];
        System.arraycopy(longs, 0, newArr, 0, i);
        System.arraycopy(longs, i + 1, newArr, i, newArr.length - i);
        return new LongArray(newArr);
    }
    
    /**
     * A {@link LongArray} factory.
     */
    static public final class Builder extends PowerlessArray.Builder<Long> {
        private long[] buffer;
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
            buffer = new long[estimate > 0 ? estimate : 32];
            size = 0;
        }

        // ArrayBuilder<Long> interface
        /**
         * Append a <code>Long</code>
         * @param newLong the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(Long newLong) {
            append ((long) newLong);
        }

        /**
         * Append an array of <code>Long</code>s
         * @param newLongs the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final Long[] newLongs) {
            append(newLongs, 0, newLongs.length);
        }      

        /**
         * Append a range of elements from an array of <code>Long</code>s
         * @param newLongs the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final Long[] newLongs, 
                          final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newLongs.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new long[newLength], 0,
                                 size);
            }
            
            for (int i = 0; i < len; ++i) {
                buffer[size + i] = newLongs[off + i];
            }           
            size = newSize;
        }
        
        /**
         * Create a snapshot of the current content.
         * @return a <code>LongArray</code> containing the elements so far
         */
        public LongArray snapshot() {
            final long[] arr;
            if (size == buffer.length) {
                arr = buffer;
            } else {
                arr = new long[size];
                System.arraycopy(buffer, 0, arr, 0, size);
            }
            return new LongArray(arr);
        }
        
        /*
         * Convenience (more efficient) methods with long
         */       
        /**
         * Append a <code>long</code>
         * @param newLong the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final long newLong) {
            if (size == buffer.length) {
                System.arraycopy(buffer, 0, buffer = new long[2 * size], 0,
                                 size);
            }
            buffer[size++] = newLong;
        }

        /**
         * Append an array of <code>long</code>s
         * @param newLongs the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final long[] newLongs) {
            append(newLongs, 0, newLongs.length);
        }      

        /**
         * Append a range of elements from an array of <code>long</code>s
         * @param newLongs the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final long[] newLongs, final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newLongs.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new long[newLength], 0,
                                 size);
            }
            System.arraycopy(newLongs, off, buffer, size, len);
            size = newSize;
        }
    }
    
    /* If one only invokes static methods statically, this is sound, since
     * LongArray extends PowerlessArray<Long> and thus this method is
     * only required to return something of a type covariant with
     * PowerlessArray.Builder<Long>.  Unfortunately, this is not completely
     * sound because it is possible to invoke static methods on instances, e.g.
     * ConstArray.Builder<String> = (ConstArray (LongArray.array())).builder(),
     * allowing for heap pollution without an unchecked cast warning.
     * 
     * The only solution to this would be to completely de-genericize these
     * methods.
     */

    /**
     * Get a <code>LongArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @return a new builder instance, with the default internal array length
     */
    @SuppressWarnings("unchecked")
    public static Builder builder() {
        return new Builder(0);
    }

    /**
     * Get a <code>LongArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @param estimate  estimated array length  
     * @return a new builder instance
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final int estimate) {
        return new Builder(estimate);
    }
}
