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
 * An immutable array of <code>boolean</code>.
 */
public final class BooleanArray extends PowerlessArray<Boolean> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient boolean[] booleans;

    private BooleanArray(boolean... booleans) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.booleans = booleans;
    }
    
    /**
     * Constructs an array of <code>boolean</code>s.
     * @param booleans each element
     */
    static public BooleanArray array(final boolean... booleans) {
        return new BooleanArray(booleans.clone());
    }
    
    /*
     * The following are necessary because otherwise calls with <=4 arguments
     * are resolved to the superclass PowerlessArray
     */
    
    /**
     * Construct an empty <code>BooleanArray</code>
     */
    @SuppressWarnings("unchecked")  // the warning here seems completely bogus
    static public BooleanArray array() {
        return new BooleanArray(new boolean[]{});
    }

    /**
     * Construct a <code>BooleanArray</code> with one element.
     * @param value    the value
     */    
    static public BooleanArray array(boolean value) {
        return new BooleanArray(new boolean[]{value});
    }
    
    /**
     * Construct a <code>BooleanArray</code> with two elements.
     * @param value1    the first value
     * @param value2    the second value
     */     
    static public BooleanArray array(boolean value1, boolean value2) {
        return new BooleanArray(new boolean[]{value1, value2});
    }
    
    /**
     * Construct a <code>BooleanArray</code> with three elements.
     * @param value1    the first value
     * @param value2    the second value
     * @param value3    the third value
     */     
    static public BooleanArray array(boolean value1, boolean value2, boolean value3) {
        return new BooleanArray(new boolean[]{value1, value2, value3});
    }
    
    /**
     * Construct a <code>BooleanArray</code> with four elements.
     * @param value1    the first value
     * @param value2    the second value
     * @param value3    the third value
     * @param value4    the fourth value
     */    
    static public BooleanArray array(boolean value1, boolean value2, boolean value3,
                                  boolean value4) {
        return new BooleanArray(new boolean[]{value1, value2, value3, value4});
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

        out.writeInt(booleans.length);
        for (boolean c : booleans) {
            out.writeBoolean(c);
        }
    }

    private void readObject(final ObjectInputStream in) throws 
                                        IOException, ClassNotFoundException {
        in.defaultReadObject();

        final int length = in.readInt();
        booleans = new boolean[length];
        for (int i = 0; i < length; ++i) {
            booleans[i] = in.readBoolean();
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
        if (other instanceof BooleanArray) {
            // Simple case: just compare booleanArr fields
            return Arrays.equals(booleans, ((BooleanArray)other).booleans);
        } else if (other instanceof ConstArray<?>) {
            // Other array does not have contents in booleanArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray<?> otherArray = (ConstArray<?>)other;
            if (otherArray.length() != booleans.length) {
                return false;
            }            
            for (int i = 0; i < booleans.length; ++i) {
                final Object otherElement = otherArray.get(i);
                if (!(otherElement instanceof Boolean) ||
                    ((Boolean)otherElement).booleanValue() != booleans[i]) {
                    return false;
                }
            }            
            return true;
        } else {
            // Only a ConstArray can be equal to a BooleanArray
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
        // their primitive values, a BooleanArray has the same hashCode as a
        // ConstArray<Boolean> with the same contents.
        return Arrays.hashCode(booleans);
    }
    
    /**
     * Return a string representation of the array
     */    
    public String toString() { 
        return Arrays.toString(booleans);
    }
    
    // org.joe_e.ConstArray interface

    /**
     * Gets the length of the array.
     */
    public int length() { 
        return booleans.length;
    }
    
    /**
     * Creates a <code>Boolean</code> for a specified <code>boolean</code>.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public Boolean get(int i) { 
        return booleans[i]; 
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
            prototype[i] = (T) (Boolean) booleans[i];
        }
        return prototype;
    }
    
    /**
     * Creates a <code>BooleanArray<code> with an appended <code>Boolean</code>.
     * @param newBoolean   the element to append
     * @throws NullPointerException <code>newBoolean</code> is null
     */
    public BooleanArray with(final Boolean newBoolean) {
        return with(newBoolean.booleanValue());
    }
           
    /*
     * Convenience (more efficient) methods with boolean
     */
        
    /**
     * Gets the <code>boolean</code> at a specified position.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public boolean getBoolean(final int i) { 
        return booleans[i]; 
    }

    /**
     * Creates a mutable copy of the <code>boolean</code> array
     */
    public boolean[] toBooleanArray() {
        return booleans.clone(); 
    }
    
    /** 
     * Creates a <code>BooleanArray</code> with an appended <code>boolean</code>.
     * @param newBoolean   the element to append
     */
    public BooleanArray with(final boolean newBoolean) {
        final boolean[] newBooleans = new boolean[booleans.length + 1];
        System.arraycopy(booleans, 0, newBooleans, 0, booleans.length);
        newBooleans[booleans.length] = newBoolean;
        return new BooleanArray(newBooleans);
    }

    /**
     * Return a new <code>BooleanArray</code> that contains the same elements
     * as this one excluding the element at a specified index
     * @param i the index of the element to exclude
     * @return  the new array
     */
    public BooleanArray without(final int i) {
        final boolean[] newArr = new boolean[booleans.length - 1];
        System.arraycopy(booleans, 0, newArr, 0, i);
        System.arraycopy(booleans, i + 1, newArr, i, newArr.length - i);
        return new BooleanArray(newArr);
    }
    
    /**
     * A {@link BooleanArray} factory.
     */
    public static final class Builder extends 
                                        PowerlessArray.Builder<Boolean> {
        private boolean[] booleanBuffer;

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
            booleanBuffer = new boolean[estimate > 0 ? estimate : 32];
            size = 0;
        }

        // ArrayBuilder<Boolean> interface
        /**
         * Append a <code>Boolean</code>
         * @param newBoolean the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(Boolean newBoolean) {
            append ((boolean) newBoolean);
        }

        /**
         * Append an array of <code>Boolean</code>s
         * @param newBooleans the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final Boolean[] newBooleans) {
            append(newBooleans, 0, newBooleans.length);
        }      

        /**
         * Append a range of elements from an array of <code>Boolean</code>s
         * @param newBooleans the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final Boolean[] newBooleans, 
                          final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newBooleans.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > booleanBuffer.length) {
                int newLength = Math.max(newSize, 2 * booleanBuffer.length);
                System.arraycopy(booleanBuffer, 0, 
                                 booleanBuffer = new boolean[newLength], 0, size);
            }
            
            for (int i = 0; i < len; ++i) {
                booleanBuffer[size + i] = newBooleans[off + i];
            }           
            size = newSize;
        }
        
        /**
         * Create a snapshot of the current content.
         * @return a <code>BooleanArray</code> containing the elements so far
         */
        public BooleanArray snapshot() {
            final boolean[] arr;
            if (size == booleanBuffer.length) {
                arr = booleanBuffer;
            } else {
                arr = new boolean[size];
                System.arraycopy(booleanBuffer, 0, arr, 0, size);
            }
            return new BooleanArray(arr);
        }
        
        /*
         * Convenience (more efficient) methods with boolean
         */       
        /**
         * Append a <code>boolean</code>
         * @param newBoolean the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final boolean newBoolean) {
            if (size == booleanBuffer.length) {
                System.arraycopy(booleanBuffer, 0,
                                 booleanBuffer = new boolean[2 * size], 0, size);
            }
            booleanBuffer[size++] = newBoolean;
        }

        /**
         * Append an array of <code>boolean</code>s
         * @param newBooleans the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final boolean[] newBooleans) {
            append(newBooleans, 0, newBooleans.length);
        }      

        /**
         * Append a range of elements from an array of <code>boolean</code>s
         * @param newBooleans the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final boolean[] newBooleans, final int off, 
                           final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newBooleans.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > booleanBuffer.length) {
                int newLength = Math.max(newSize, 2 * booleanBuffer.length);
                System.arraycopy(booleanBuffer, 0, 
                                 booleanBuffer = new boolean[newLength], 0, size);
            }
            System.arraycopy(newBooleans, off, booleanBuffer, size, len);
            size = newSize;
        }
    }
    
    /* If one only invokes static methods statically, this is sound, since
     * ByteArray extends PowerlessArray<Byte> and thus this method is
     * only required to return something of a type covariant with
     * PowerlessArray.Builder<Byte>.  Unfortunately, this is not completely
     * sound because it is possible to invoke static methods on instances, e.g.
     * ConstArray.Builder<String> = (ConstArray (BooleanArray.array())).builder()
     * Invocations of append() can then throw ClassCastExceptions.
     * 
     * I can't see a way to avoid this other than to de-genericize everything.
     */

    /**
     * Get a <code>BooleanArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @return a new builder instance, with the default internal array length
     */
    @SuppressWarnings("unchecked")
    public static Builder builder() {
        return new Builder(0);
    }

    /**
     * Get a <code>BooleanArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @param estimate  estimated array length  
     * @return a new builder instance
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final int estimate) {
        return new Builder(estimate);
    }
}
