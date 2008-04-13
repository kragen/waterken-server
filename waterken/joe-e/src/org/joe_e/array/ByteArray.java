// Copyright 2006-08 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e.array;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.lang.reflect.Array;

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
     * Constructs a <code>ByteArray</code>.
     * @param bytes each <code>byte</code>
     */
    static public ByteArray array(final byte... bytes) {
        return new ByteArray(bytes.clone());
    }
    
    // java.io.Serializable interface
    
    /*
     * Serialization hacks to prevent the contents from being serialized as a
     * mutable array.  This improves efficiency for projects that serialize
     * Joe-E objects using Java's serialization API by avoiding treatment of
     * immutable state as mutable.  These methods can otherwise be ignored.
     *
     * These use the byte array directly, for better performance than would
     * be obtained by using the element-by-element versions from ConstArray.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private void readObject(final ObjectInputStream in) throws 
                                        IOException, ClassNotFoundException {
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
     * as {@link java.util.Arrays#hashCode(Object[])} called on a Java array
     * containing the same elements.
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
     * Creates a <code>ByteArray</code> with an appended <code>Byte</code>.
     * @param newByte   the element to append
     * @throws NullPointerException <code>newByte</code> is null
     */
    public ByteArray with(final Byte newByte) {
        return with((byte) newByte);
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
     * Creates a <code>ByteArray</code> with an appended <code>byte</code>.
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
   public InputStream asInputStream() {
       return new ByteArrayInputStream(bytes);
   }
   
   /**
    * Return a new <code>ByteArray</code> that contains the same elements
    * as this one excluding the element at a specified index
    * @param i the index of the element to exclude
    * @return  the new array
    */
   public ByteArray without(final int i) {
       final byte[] newArr = new byte[bytes.length - 1];
       System.arraycopy(bytes, 0, newArr, 0, i);
       System.arraycopy(bytes, i + 1, newArr, i, newArr.length - i);
       return new ByteArray(newArr);
   }
   
   /**
    * A {@link ByteArray} factory.
    */
   static public final class Builder extends PowerlessArray.Builder<Byte> {
       private byte[] byteBuffer;

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
           byteBuffer = new byte[estimate > 0 ? estimate : 32];
           size = 0;
       }

       // ArrayBuilder<Byte> interface      
       /**
        * Append a <code>Byte</code>
        * @param newByte the element to add
        * @throws IndexOutOfBoundsException if the resulting array would exceed
        *  the maximum length of a Java array.  The builder is unmodified.
        */
       public void append(Byte newByte) {
           append((byte) newByte);
       }
       
       /**
        * Append an array of <code>Bytes</code>s
        * @param newBytes the elements to add
        * @throws IndexOutOfBoundsException if the resulting internal array
        *  would exceed the maximum length of a Java array.  The builder is
        *  unmodified.
        */
       public void append(final Byte[] newBytes) {
           append(newBytes, 0, newBytes.length);
       }      
       
       /**
        * Append a range of elements from an array of <code>Byte</code>s
        * @param newBytes the array to add elements from
        * @param off the index of the first element to add
        * @param len the number of elements to add
        * @throws IndexOutOfBoundsException if an out-of-bounds index would
        *  be referenced or the resulting internal array would exceed the
        *  maximum length of a Java array.  The builder is unmodified.
        */
       public void append(Byte[] newBytes, int off, int len) {
           int newSize = size + len;
           if (newSize < 0 || off < 0 || len < 0 || off + len < 0
               || off + len > newBytes.length) {
               throw new IndexOutOfBoundsException();
           }
           if (newSize > byteBuffer.length) {
               int newLength = Math.max(newSize, 2 * byteBuffer.length);
               System.arraycopy(byteBuffer, 0, 
                                byteBuffer = new byte[newLength], 0, size);
           }
           
           for (int i = 0; i < len; ++i) {
               byteBuffer[size + i] = newBytes[off + i];
           }           
           size = newSize;
       }
       
       /**
        * Create a snapshot of the current content.
        * @return a <code>ByteArray</code> containing the elements so far
        */
       public ByteArray snapshot() {
           final byte[] arr;
           if (size == byteBuffer.length) {
               arr = byteBuffer;
           } else {
               arr = new byte[size];
               System.arraycopy(byteBuffer, 0, arr, 0, size);
           }
           return new ByteArray(arr);
       }
       
       /*
        * Convenience (more efficient) methods with byte
        */       
       /**
        * Append a <code>byte</code>
        * @param newByte the element to add
        * @throws IndexOutOfBoundsException if the resulting internal array
        *  would exceed the maximum length of a Java array.  The builder is
        *  unmodified.
        */
       public void append(final byte newByte) {
           if (size == byteBuffer.length) {
               System.arraycopy(byteBuffer, 0, 
                                byteBuffer = new byte[2 * size], 0, size);
           }
           byteBuffer[size++] = (byte) newByte;
       }
       
       /**
        * Append an array of <code>byte</code>s
        * @param newBytes the elements to add
        * @throws IndexOutOfBoundsException if the resulting internal array
        *  would exceed the maximum length of a Java array.  The builder is
        *  unmodified.
        */
       public void append(final byte[] newBytes) {
           append(newBytes, 0, newBytes.length);
       }      
       
       /**
        * Append a range of elements from an array of <code>byte</code>s
        * @param newBytes the array to add elements from
        * @param off the index of the first element to add
        * @param len the number of elements to add
        * @throws IndexOutOfBoundsException if an out-of-bounds index would
        *  be referenced or the resulting internal array would exceed the
        *  maximum length of a Java array.  The builder is unmodified.
        */
       public void append(byte[] newBytes, int off, int len) {
           int newSize = size + len;
           if (newSize < 0 || off < 0 || len < 0 || off + len < 0
               || off + len > newBytes.length) {
               throw new IndexOutOfBoundsException();
           }
           if (newSize > byteBuffer.length) {
               int newLength = Math.max(newSize, 2 * byteBuffer.length);
               System.arraycopy(byteBuffer, 0,
                                byteBuffer = new byte[newLength], 0, size);
           }
           System.arraycopy(newBytes, off, byteBuffer, size, len);
           size = newSize;
       }
       
       /**
        * Convenience method that creates an output stream using this Builder 
        * @return a new output stream that wraps this Builder
        */
       public BuilderOutputStream asOutputStream() {
           return new BuilderOutputStream(this);
       }
   }
   
   /**
    * A {@link ByteArray} factory that extends {@link OutputStream}.
    * All methods are simple wrappers around those provided by
    * {@link ByteArray.Builder}.
    */
   static public final class BuilderOutputStream extends OutputStream {
       private final Builder builder;
       
       /**
        * Create an output stream using a new underlying {@link Builder} with
        * the default internal array length
        */
       public BuilderOutputStream() {
           builder = new Builder();
       }

       /**
        * Create an output stream using a new underlying {@link Builder}
        * @param estimate estimated array length
        */
       public BuilderOutputStream(int estimate) {
           builder = new Builder(estimate);
       }

       /**
        * Create an output stream that wraps the specified {@link Builder}
        * @param toWrap the <code>Builder</code> to wrap
        */
       public BuilderOutputStream(Builder toWrap) {
           builder = toWrap;
       }
       
       // OutputStream interface
       /**
        * Append a <code>byte</code> to the underlying {@link Builder}
        * @param b the element to add
        */
       public void write(int b) {
           builder.append((byte) b);
       }

       // Should be equivalent to OutputStream's implementation, but provided
       // so that people don't have to catch IOException
       /**
        * Append a <code>byte</code> array to the underlying {@link Builder}
        * @param b the elements to add
        */
       public void write(byte[] b) {
           builder.append(b, 0, b.length);
       }

       /**
        * Append part of a <code>byte</code> array to the underlying 
        * {@link Builder}
        * @param b the elements to add
        * @param off the index of the first element to add
        * @param len the number of elements to add
        */
       public void write(byte[] b, int off, int len) {
           builder.append(b, off, len);
       }
     
       // Added methods to get data out
       /** 
        * Gets the number of bytes written to the underlying {@link Builder}
        * @return the number of elements that have been appended
        */
       public int length() {
           return builder.length();
       }
              
       /**
        * Create a snapshot of the current content.
        * @return a <code>ByteArray</code> containing the elements so far
        */
       public ByteArray snapshot() {
           return builder.snapshot();
       }
   }

   /* If one only invokes static methods statically, this is sound, since
    * ByteArray extends PowerlessArray<Byte> and thus this method is
    * only required to return something of a type covariant with
    * PowerlessArray.Builder<Byte>.  Unfortunately, this is not completely
    * sound because it is possible to invoke static methods on instances, e.g.
    * ConstArray.Builder<String> = (ConstArray (ByteArray.array())).builder().
    * Invocations of append() can then throw ClassCastExceptions.
    * 
    * I can't see a way to avoid this other than to de-genericize everything.
    */
   
   /**
    * Get a <code>ByteArray.Builder</code>.  This is equivalent to the
    * constructor.
    * @return a new builder instance, with the default internal array length
    */
   @SuppressWarnings("unchecked")
   public static Builder builder() {
       return new Builder();
   }

   /**
    * Get a <code>ByteArray.Builder</code>.  This is equivalent to the
    * constructor.
    * @param estimate  estimated array length  
    * @return a new builder instance
    */
   @SuppressWarnings("unchecked")
   public static Builder builder(final int estimate) {
       return new Builder(estimate);
   }
}
