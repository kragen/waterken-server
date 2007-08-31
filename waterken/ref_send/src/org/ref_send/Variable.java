// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send;

/**
 * A variable.
 * <p>
 * Not all variables are mutable. A read-only variable will throw an exception
 * from its {@link #put put} implementation.
 * </p>
 * @param <T> value type
 */
public interface
Variable<T> {
    
    /**
     * Gets the current value.
     * @throws Exception    any problem
     */
    T
    get() throws Exception;
    
    /**
     * Assigns a new value.
     * <p>The implementation of this method is expected to be idempotent,
     * meaning the effect of calling it multiple times in a row with the same
     * argument must be the same as calling it once.</p>
     * @param value new value
     * @throws Exception    any problem
     */
    void
    put(T value) throws Exception;
}
