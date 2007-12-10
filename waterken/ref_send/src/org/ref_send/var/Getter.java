// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

import org.ref_send.promise.Volatile;

/**
 * The {@linkplain Variable#getter read} facet of a {@link Variable}.
 * @param <T> value type
 */
public interface
Getter<T> {
    
    /**
     * Gets the current value.
     */
    Volatile<T>
    get();
}
