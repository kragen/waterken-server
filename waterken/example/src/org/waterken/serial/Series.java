// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

/**
 * An open ended series of elements. 
 */
public interface
Series<T> extends Iterable<T> {

    /**
     * Appends a value to the end of the series.
     * @param value value to append
     */
    void
    produce(T value);
    
    /**
     * Removes the first element in the series.
     * @return value of the removed element
     */
    T
    consume();
}
