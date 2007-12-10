// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

/**
 * The {@linkplain Variable#setter write} facet of a {@link Variable}.
 * @param <T> value type
 */
public interface
Setter<T> {
    
    /**
     * Assigns a value.
     * <p>
     * The implementation of this method is expected to be idempotent, meaning
     * the effect of calling it multiple times in a row with the same argument
     * must be the same as calling it once.
     * </p>
     * @param value value to assign
     */
    void
    set(T value);
}
