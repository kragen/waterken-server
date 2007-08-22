// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

/**
 * An element in a series.
 */
public interface
Element<T> {

    /**
     * element value
     */
    T
    getValue();

    /**
     * next element
     */
    Element<T>
    getNext();
}
