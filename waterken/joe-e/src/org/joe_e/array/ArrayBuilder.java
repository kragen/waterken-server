// Copyright 2008 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.array;

/**
 * A builder of ConstArrays.  Each type of ConstArray has a nested class that
 * implements this interface, e.g. <code>ConstArray.Builder</code>.
 * 
 * @author Adrian Mettler
 *
 * @param <E> the component type of the Array to build
 */
public interface ArrayBuilder<E> {
    /** 
     * Appends an element to the Array
     * @param element the element to append
     */
    void append(E element);

    /** 
     * Appends all elements from a Java array to the Array
     * @param elements the element to append
     */
    void append(E[] elements);

    /** 
     * Appends a range of elements from a Java array to the Array
     * @param elements the source array
     * @param off   the index of the first element to append
     * @param len   the number of elements to append
     */
    void append(E[] elements, int off, int len);
    
    /** 
     * Gets the current number of elements in the Array
     * @return the number of elements that have been appended
     */
    int length();
    
    /**
     * Create a snapshot of the current content.
     * @return a <code>ConstArray<E></code> containing the elements written
     *         so far
     */
    ConstArray<E> snapshot();
}
