// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import org.ref_send.promise.Promise;

/**
 * An infinite series of elements.
 * <p>
 * A series is a FIFO list of values where the values can be removed from the
 * list before they have been added. An invocation of {@link #consume} returns
 * a promise for what will be the next element in the list, once it is added, at
 * which time it will already have been removed. ;)
 * </p> 
 */
public interface
Series<T> extends Iterable<Promise<T>> {

    /**
     * Appends a value to the end of the series.
     * @param value value to append
     */
    void produce(Promise<T> value);
    
    /**
     * Removes the first element in the series.
     * @return value of the removed element
     */
    Promise<T> consume();
}
