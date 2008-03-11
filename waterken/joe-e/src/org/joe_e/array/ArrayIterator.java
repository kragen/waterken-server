// Copyright 2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e.array;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator for ConstArrays. Needed in order for ConstArray and its
 * subclasses to support the Iterable interface and be usable with the new 
 * for-loop syntax.
 *
 * @param <E> the element type of the ConstArray being iterated
 */
final class ArrayIterator<E> implements Iterator<E>, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final ConstArray<E> arr;
	private final int length;
    private int pos;     // the next position to return the contents of
	
    /**
     * Create an ArrayIterator to iterate over the specified ConstArray
     * @param arr the array to iterate over
     */
	ArrayIterator(final ConstArray<E> arr) {
		this.arr = arr;
		this.length = arr.length();
        this.pos = 0; 
	}
	
    /**
     * Returns true if the iteration has more elements. 
     * (In other words, returns true if next would return an element rather
     *  than throwing an exception.)
     * @return true if the iterator has more elements.
     */
	public boolean hasNext() { 
        return pos != length; 
    }
	
    /**
     * Gets the next element in the array.
     * @throws NoSuchElementException if the end of the array has been reached.
     */
    public E next() {
        if (pos == length) { 
            throw new NoSuchElementException();
        }
		return arr.get(pos++);
	}

    /**
     * Remove is not supported by this iterator.
     * @throws UnsupportedOperationException
     */
	public void remove() { 
        throw new UnsupportedOperationException(); 
    }
}
