// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send;

import java.io.Serializable;

/**
 * A simple variable.
 * @param <T> value type
 */
public final class
Slot<T> implements Variable<T>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * current value
     */
    private T value;
    
    private
    Slot(final T value) {
        this.value = value;
    }

    /**
     * Constructs an instance.
     * @param <T> value type
     * @param value initial value
     */
    static public <T> Slot<T>
    var(final T value) { return new Slot<T>(value); }
    
    // org.ref_send.Variable interface
    
    public T
    get() { return value; }
    
    public void
    put(final T value) {
        this.value = value;
    }
}
