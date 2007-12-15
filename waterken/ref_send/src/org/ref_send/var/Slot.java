// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

import java.io.Serializable;

import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;

/**
 * A simple {@link Variable} implementation.
 */
final class
Slot<T> implements Getter<T>, Serializable {
    static private final long serialVersionUID = 1L;

    protected T value;
    
    protected
    Slot() {}
    
    // org.ref_send.var.Getter interface
    
    public Volatile<T>
    get() { return Eventual.promised(value); }
    
    // org.ref_send.var.Slot interface
    
    protected void
    set(final T value) { this.value = value; }
}
