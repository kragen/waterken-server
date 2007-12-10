// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.var.Variable;

/**
 * An editable list of {@link Variable}s.
 * @param <T> value type
 */
public interface
Menu<T> {

    /**
     * Gets a snapshot of the current variable values.
     */
    Promise<ConstArray<T>>
    getSnapshot();
    
    /**
     * Generates a new {@link #getEntries entry}.
     */
    Promise<Variable<T>>
    grow();
}
