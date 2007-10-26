// Copyright 2006 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e.array;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.Arrays;


/**
 * An immutable array of <code>byte</code>.
 */
public final class ByteArray extends PowerlessArray<Byte> {
    static private final long serialVersionUID = 1L;   
    
    private /* final */ transient byte[] bytes;

    private ByteArray(byte... bytes) {
        // Use back door constructor that sets backing store to null.
        // This lets ConstArray's methods know not to use the backing
        // store for accessing this object.
        super(null);
        this.bytes = bytes;
    }
    
    /**
     * Constructs a {@link ByteArray}.
     * @param bytes each <code>byte</code>
     */
    static public ByteArray array(final byte... bytes) {
        return new ByteArray(bytes.clone());
    }
    
    /**
     * A {@link ByteArray} factory.
     */
    static public final class
    Generator extends OutputStream {
        
        private byte[] buffer;  // buffer.length >= 1
        private int size;       // size <= buffer.length
        
        /**
         * Constructs an instance.
         * @param estimate  estimated array length
         */
        public
        Generator(final int estimate) {
            buffer = new byte[0 == estimate ? 32 : estimate];
            size = 0;
        }

        // java.io.OutputStream interface
        
        public void
        write(final int b) {
            if (size == buffer.length) {
                System.arraycopy(buffer, 0, buffer = new byte[2*size], 0, size);
            }
            buffer[size++] = (byte)b;
        }

        public void
        write(final byte[] b, final int off, final int len) {
            int newSize = size + len;
            if (len < 0 || newSize < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (newSize > buffer.length) {
                int newLength = Math.max(newSize, 2 * buffer.length);
                System.arraycopy(buffer, 0, buffer = new byte[newLength], 0,
                                 size);
            }
            System.arraycopy(b, off, buffer, size, len);
            size = newSize;
        }
        
        // org.joe_e.array.ByteArray.Generator interface
        
        /**
         * Creates a snapshot of the current content.
         */
        public ByteArray
        snapshot() {
            final byte[] r;
            if (buffer.length == size) {
                r = buffer;
            } else {
                r = new byte[size];
                System.arraycopy(buffer, 0, r, 0, size);
            }
            return new ByteArray(r);
        }
    }
    
    // java.io.Serializable interface
    
    /*
     * Serialization hacks to prevent the contents from being serialized as a
     * mutable array.  This improves efficiency for projects that serialize
     * Joe-E objects using Java's serialization API by avoiding treatment of
     * immutable state as mutable.  These methods can otherwise be ignored.
     *
     * These use the byte array directly, for better performance than would
     * be obtained by using the element-by-element versions from CharArray.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private void readObject(final ObjectInputStream in) throws IOException, 
    						                      ClassNotFoundException {
        in.defaultReadObject();

        final int length = in.readInt();
        bytes = new byte[length];
        in.readFully(bytes);
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
        if (other instanceof ByteArray) {
            // Simple case: just compare byteArr fields
            return Arrays.equals(bytes, ((ByteArray)other).bytes);
        } else if (other instanceof ConstArray) {
            // Other array does not have contents in byteArr:
            // check that length matches, and then compare elements one-by-one
            final ConstArray otherArray = (ConstArray)other;
            if (otherArray.length() != bytes.length) {
                return false;
            }            
            for (int i = 0; i < bytes.length; ++i) {
                final Object otherElement = otherArray.get(i);
                if (!(otherElement instanceof Byte) ||
                    ((Byte)otherElement).byteValue() != bytes[i]) {
                    return false;
                }
            }            
            return true;
        } else {
            // Only a ConstArray can be equal to a ByteArray
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
        // their primitive values, a ByteArray has the same hashCode as a
        // ConstArray<Byte> with the same contents.
        return Arrays.hashCode(bytes);
    }
    
    /**
     * Return a string representation of the array
     */    
    public String toString() { 
        return Arrays.toString(bytes);
    }
    
    // org.joe_e.ConstArray interface

    /**
     * Gets the length of the array.
     */
    public int length() { 
        return bytes.length;
    }
    
    /**
     * Creates a {@link Byte} for a specified <code>byte</code>.
     * @param i position of the <code>byte</code> to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public Byte get(int i) { 
        return bytes[i]; 
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
            prototype[i] = (T) (Byte) bytes[i];
        }
        return prototype;
    }
    
    /**
     * Creates a {@link ByteArray} with an appended {@link Byte}.
     * @param newByte   the {@link Byte} to append
     * @throws NullPointerException <code>newByte</code> is null
     */
    public ByteArray with(final Byte newByte) {
        return with(newByte.byteValue());
    }
           
    /*
     * Convenience (more efficient) methods with byte
     */
        
    /**
     * Gets the <code>byte</code> at a specified position.
     * @param i position of the <code>byte</code> to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    public byte getByte(final int i) { 
        return bytes[i]; 
    }

    /**
     * Creates a mutable copy of the <code>byte</code> array
     */
    public byte[] toByteArray() {
        return bytes.clone(); 
    }
    
    /** 
     * Creates a {@link ByteArray} with an appended <code>byte</code>.
     * @param newByte   the <code>byte</code> to append
     */
    public ByteArray with(final byte newByte) {
        final byte[] newBytes = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        newBytes[bytes.length] = newByte;
        return new ByteArray(newBytes);
    }
    
    /**
     * Views this array as an input stream.
     */
    public InputStream open() {return new ByteArrayInputStream(bytes);}
}
