// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;

/**
 * An editable list of values.
 * @param <T> value type
 */
public interface
Menu<T> {

    /**
     * Gets the entries.
     */
    Promise<ConstArray<T>>
    getEntries();
    
    /**
     * Generates a new {@link #getEntries entry}.
     * @return new entry
     */
    Promise<T>
    grow();
    
    /**
     * Removes an {@link #getEntries entry}.
     * @param index index of {@link #getEntries entry} to be removed
     */
    void
    remove(int index);
}
