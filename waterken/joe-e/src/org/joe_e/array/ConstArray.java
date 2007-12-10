// Copyright 2006-2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * 
 * @author Adrian Mettler 
 */
package org.joe_e.array;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;

import org.joe_e.JoeE;
import org.joe_e.Selfless;

/**
 * A read-only array containing elements of an arbitrary type.
 * <p>
 * Note: this class implements Serializable in order to avoid preventing
 * trusted (non-Joe-E) code from serializing it.  The Java Serialization API
 * is tamed away as unsafe, and thus is not available to Joe-E code.
 *
 * @param <E> the element type of objects contained in the array
 */
public class ConstArray<E> implements Selfless, Iterable<E>, Serializable {
    static private final long serialVersionUID = 1L;
    
    // Marked transient to hide from serialization; see writeObject()
    // This field should act as if final.
    // This array should only contain objects of type E provided that clients
    // avoid generics typesafety violations.
    transient /* final */ Object[] arr;
    
    
    /**
     * Package-scope back-door constructor for use by subclasses that override
     * all methods that make use of the field arr.  Nullity of arr is used to
     * distinguish between instances with which this class must interact by
     * using the public interface rather than through their arr field.
     */
    ConstArray(final Object[] arr) {
        this.arr = arr;
    }
    
    /**
     * Constuct a {@link ConstArray}.
     * @param values    each value
     */
    static public <T> ConstArray<T> array(final T... values) {
        return new ConstArray<T>(values.clone());
    }
    
    // java.io.Serializable interface
    
    /*
     * Serialization hacks to prevent the contents from being serialized as
     * a mutable array.  This improves efficiency for projects that serialize
     * Joe-E objects using Java's serialization API to avoid treating
     * immutable state as mutable.  They can otherwise be ignored.
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        if (null == arr) {
            out.writeObject(null);
        } else {
            out.writeObject(arr.getClass().getComponentType());
            out.writeInt(arr.length);
            for (final Object element : arr) {
                out.writeObject(element);
            }
        }
    }

    private void readObject(final ObjectInputStream in) throws IOException,
                                                        ClassNotFoundException {
        in.defaultReadObject();

        final Class e = (Class)in.readObject();
        if (null == e) {
            arr = null;
        } else {
            final int length = in.readInt();
            final Object[] arr = (Object[])Array.newInstance(e, length);
            for (int i = 0; i != length; ++i) {
                arr[i] = in.readObject();
            }
            this.arr = arr;
        }
    }
    
    // java.lang.Object interface
    
    /**
     * Test for equality with another object.
     * 
     * @return true if the other object is a ConstArray with the same contents
     * as this array (determined by calling equals() on array elements)
     */ 
    public boolean equals(final Object other) {
        // Can't be equal if not a ConstArray
        if (!(other instanceof ConstArray)) {
            return false;
        }
        ConstArray otherArray = (ConstArray) other;
        // check that length matches
        if (arr.length != otherArray.length()) {
            return false;
        }        

        // Compare elements, either both null or equals()
        for (int i = 0; i < arr.length; ++i) {
            if (arr[i] == null && otherArray.get(i) != null
                || arr[i] != null && !arr[i].equals(otherArray.get(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * Computes a digest of the array for hashing.  If all of the elements of
     * the array implement Selfless in the overlay type system, the hash will
     * be the same as that computed by Arrays.hashCode() for a Java array with
     * the same elements.  The precise behavior when some elements are not 
     * Selfless is unspecified, and may change in future releases.
     * 
     * @return a hash code based on the contents of this array
     */
    public int hashCode() {
        int hashCode = 1;
        for (final Object i : arr) {
            hashCode *= 31;
            // treat non-Selfless as nulls
            if (JoeE.instanceOf(i, Selfless.class)) {
                hashCode += i.hashCode();
            }
        }

        return hashCode;
    }
    
    /**
     * Return a string representation of the array.  Equivalent to the result
     * of Arrays.toString() except that some elements may be replaced with the
     * string "&lt;unprintable&gt;".  The set of elements for which this is
     * the case is unspecified, and may change in future releases. 
     * 
     * @return a string representation of this array
     */
    /*
     * TODO: Change this to support more types, either through
     * the introduction of an interface for toString()able objects
     * or through reflection.
     */
    public String toString() {
        StringBuilder text = new StringBuilder("[");
        boolean first = true;
        for (Object element : arr) {
            if (first) {
                first = false;
            } else {
                text.append(", ");
            }
            
            if (element == null) {
                text.append("null");
            }
            if (element instanceof String || element instanceof ConstArray
                || element instanceof Boolean || element instanceof Byte  
                || element instanceof Character || element instanceof Double
                || element instanceof Float || element instanceof Integer
                || element instanceof Long || element instanceof Short) {
                text.append(element.toString());
            } else {
                text.append("<unprintable>");
            }           
        }
       
        return text.append("]").toString();       
    }
    
    // java.lang.Iterable interface

    /**
     * Return a new iterator over the array
     */
    public ArrayIterator<E> iterator() { 
        return new ArrayIterator<E>(this);
    }
    
    // org.joe_e.ConstArray interface
      
    /**
     * Gets the element at a specified position.
     * @param i position of the element to return
     * @throws ArrayIndexOutOfBoundsException <code>i</code> is out of bounds
     */
    @SuppressWarnings("unchecked")
    public E get(int i) { 
        return (E) arr[i];
    }
     
    /**
     * Return the length of the array
     */
    public int length() {
        return arr.length;
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
        
        System.arraycopy(arr, 0, prototype, 0, len);
        return prototype;
    }
        
    /**
     * Return a new <code>ConstArray</code> that contains the same elements
     * as this one but with a new element added to the end
     * @param newE the element to add
     * @return the new array
     */
    public ConstArray<E> with(E newE) {
        // We use a new Object array here, because we don't know the static type
        // of E that was used; it may not match the dynamic component type of
        // arr due to array covariance.
        final Object[] newArr = new Object[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        newArr[arr.length] = newE;
        return new ConstArray<E>(newArr);       
    }
    
    public ConstArray<E> without(final int i) {
        final Object[] newArr = new Object[arr.length - 1];
        System.arraycopy(arr, 0, newArr, 0, i);
        System.arraycopy(arr, i + i, newArr, i, newArr.length - i);
        return new ConstArray<E>(newArr);
    }
}
