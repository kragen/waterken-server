// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import org.ref_send.promise.Volatile;

/**
 * An element in a series.
 * @param <T> {@link #getValue} type
 */
public interface
Element<T> {

    /**
     * element value
     */
    Volatile<T> getValue();

    /**
     * next element
     */
    Element<T> getNext();
}
