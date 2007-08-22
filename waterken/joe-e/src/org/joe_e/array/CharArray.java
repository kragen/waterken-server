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
     * Constructs a {@link CharArray}.
     * @param chars each <code>char</code>
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
     * as <code>Arrays.hashCode()</code> called on a Java array containing the
     * same elements.
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
     * Creates a {@link Character} for a specified <code>char</code>.
     * @param i position of the <code>char</code> to return
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
     * Creates a {@link CharArray} with an appended {@link Character}.
     * @param newChar   the {@link Character} to append
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
     * @param i position of the <code>char</code> to return
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
     * Creates a {@link CharArray} with an appended <code>char</code>.
     * @param newChar   the <code>char</code> to append
     */
    public CharArray with(final char newChar) {
        final char[] newChars = new char[chars.length + 1];
        System.arraycopy(chars, 0, newChars, 0, chars.length);
        newChars[chars.length] = newChar;
        return new CharArray(newChars);
    }
}
