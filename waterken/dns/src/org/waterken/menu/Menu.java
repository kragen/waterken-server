// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;

/**
 * An editable list of values.
 * @param <T> value type
 */
public interface
Menu<T> {

    /**
     * Gets a snapshot of the current variable values.
     */
    Promise<Snapshot<T>> getSnapshot();
    
    /**
     * Appends a new entry.
     * @param initial   initial value
     * @return corresponding writer
     */
    Receiver<T> grow();
    
    /**
     * Removes an entry.
     * @param entry     entry to remove 
     */
    void remove(final Receiver<T> entry);
}
