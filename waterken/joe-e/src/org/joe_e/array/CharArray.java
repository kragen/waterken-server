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
 * An immutable array of <code>char</code>.
 */
public final class CharArray extends PowerlessArray<Character> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient char[] chars;

    private CharArray(char... chars) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.chars = chars;
    }
    
    /**
     * Constructs an array of <code>char</code>s.
     * @param chars each element
     */
    static public CharArray array(final char... chars) {
        return new CharArray(chars.clone());
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

        out.writeInt(chars.length);
        for (char c : chars) {
            out.writeChar(c);
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException, 
    						                      ClassNotFoundException {
        in.defaultReadObject();

        final int length = in.readInt();
        chars = new char[length];
        for (int i = 0; i < length; ++i) {
            chars[i] = in.readChar();
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
        if (other instanceof CharArray) {
            // Simple case: just compare charArr fields
            return Arrays.equals(chars, ((CharArray)other).chars);
        } else if (other instanceof ConstArray) {
            // Other array does not have contents in charArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray otherArray = (ConstArray)other;
            if (otherArray.length() != chars.length) {
                return false;
            }            
            for (int i = 0; i < chars.length; ++i) {
                final Object otherElement = otherArray.get(i);
                if (!(otherElement instanceof Character) ||
                    ((Character)otherElement).charValue() != chars[i]) {
                    return false;
                }
            }            
            return true;
        } else {
            // Only a ConstArray can be equal to a CharArray
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
        // their primitive values, a CharArray has the same hashCode as a
        // ConstArray<Character> with the same contents.
        return Arrays.hashCode(chars);
    }
    
    /**
     * Return a string representation of the array
     */    
    public String toString() { 
        return Arrays.toString(chars);
    }
    
    // org.joe_e.ConstArray interface

    /**
     * Gets the length of the array.
     */
    public int length() { 
        return chars.length;
    }
    
    /**
     * Creates a <code>Character</code> for a specified <code>char</code>.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public Character get(int i) { 
        return chars[i]; 
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
            prototype[i] = (T) (Character) chars[i];
        }
        return prototype;
    }
    
    /**
     * Creates a <code>CharArray<code> with an appended <code>Character</code>.
     * @param newChar   the element to append
     * @throws NullPointerException <code>newChar</code> is null
     */
    public CharArray with(final Character newChar) {
        return with(newChar.charValue());
    }
           
    /*
     * Convenience (more efficient) methods with char
     */
        
    /**
     * Gets the <code>char</code> at a specified position.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public char getChar(final int i) { 
        return chars[i]; 
    }

    /**
     * Creates a mutable copy of the <code>char</code> array
     */
    public char[] toCharArray() {
        return chars.clone(); 
    }
    
    /** 
     * Creates a <code>CharArray</code> with an appended <code>char</code>.
     * @param newChar   the element to append
     */
    public CharArray with(final char newChar) {
        final char[] newChars = new char[chars.length + 1];
        System.arraycopy(chars, 0, newChars, 0, chars.length);
        newChars[chars.length] = newChar;
        return new CharArray(newChars);
    }

    /**
     * Return a new <code>CharArray</code> that contains the same elements
     * as this one excluding the element at a specified index
     * @param i the index of the element to exclude
     * @return  the new array
     */
    public CharArray without(final int i) {
        final char[] newArr = new char[chars.length - 1];
        System.arraycopy(chars, 0, newArr, 0, i);
        System.arraycopy(chars, i + 1, newArr, i, newArr.length - i);
        return new CharArray(newArr);
    }
    
    /**
     * A {@link CharArray} factory.
     */
    static public final class Builder extends PowerlessArray.Builder<Character> {
        private char[] buffer;
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
            buffer = new char[estimate > 0 ? estimate : 32];
            size = 0;
        }

        // ArrayBuilder<Character> interface
        /**
         * Append a <code>Character</code>
         * @param newChar the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(Character newChar) {
            append ((char) newChar);
        }

        /**
         * Append an array of <code>Character</code>s
         * @param newChars the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final Character[] newChars) {
            append(newChars, 0, newChars.length);
        }      

        /**
         * Append a range of elements from an array of <code>Character</code>s
         * @param newChars the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final Character[] newChars, 
                          final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newChars.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new char[newLength], 0,
                                 size);
            }
            
            for (int i = 0; i < len; ++i) {
                buffer[size + i] = newChars[off + i];
            }           
            size = newSize;
        }
        
        /**
         * Create a snapshot of the current content.
         * @return a <code>CharArray</code> containing the elements so far
         */
        public CharArray snapshot() {
            final char[] arr;
            if (size == buffer.length) {
                arr = buffer;
            } else {
                arr = new char[size];
                System.arraycopy(buffer, 0, arr, 0, size);
            }
            return new CharArray(arr);
        }
        
        /*
         * Convenience (more efficient) methods with char
         */       
        /**
         * Append a <code>char</code>
         * @param newChar the element to add
         * @throws NegativeArraySizeException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final char newChar) {
            if (size == buffer.length) {
                System.arraycopy(buffer, 0, buffer = new char[2 * size], 0,
                                 size);
            }
            buffer[size++] = newChar;
        }

        /**
         * Append an array of <code>char</code>s
         * @param newChars the elements to add
         * @throws IndexOutOfBoundsException if the resulting internal array
         *   would exceed the maximum length of a Java array.  The builder is
         *   unmodified.
         */
        public void append(final char[] newChars) {
            append(newChars, 0, newChars.length);
        }      

        /**
         * Append a range of elements from an array of <code>char</code>s
         * @param newChars the array to add elements from
         * @param off the index of the first element to add
         * @param len the number of elements to add
         * @throws IndexOutOfBoundsException if an out-of-bounds index would
         *  be referenced or the resulting internal array would exceed the
         *  maximum length of a Java array.  The builder is unmodified.
         */
        public void append(final char[] newChars, final int off, final int len) {
            int newSize = size + len;
            if (newSize < 0 || off < 0 || len < 0 || off + len < 0
                || off + len > newChars.length) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new char[newLength], 0,
                                 size);
            }
            System.arraycopy(newChars, off, buffer, size, len);
            size = newSize;
        }
    }
    
    /* If one only invokes static methods statically, this is sound, since
     * CharArray extends PowerlessArray<Character> and thus this method is
     * only required to return something of a type covariant with
     * PowerlessArray.Builder<Character>.  Unfortunately, this is not completely
     * sound because it is possible to invoke static methods on instances, e.g.
     * ConstArray.Builder<String> = (ConstArray (CharArray.array())).builder(),
     * allowing for heap pollution without an unchecked cast warning.
     * 
     * The only solution to this would be to completely de-genericize these
     * methods.
     */

    /**
     * Get a <code>CharArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @return a new builder instance, with the default internal array length
     */
    @SuppressWarnings("unchecked")
    public static Builder builder() {
        return new Builder(0);
    }

    /**
     * Get a <code>CharArray.Builder</code>.  This is equivalent to the
     * constructor.
     * @param estimate  estimated array length  
     * @return a new builder instance
     */
    @SuppressWarnings("unchecked")
    public static Builder builder(final int estimate) {
        return new Builder(estimate);
    }
}
