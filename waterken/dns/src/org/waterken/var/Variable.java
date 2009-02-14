// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.var;

import java.io.Serializable;

import org.ref_send.promise.Receiver;

/**
 * A variable.
 * @param <T> value type
 */
public final class
Variable<T> implements Receiver<T>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * current value
     */
    private T value;
    
    /**
     * condition on {@linkplain #run assignment}
     */
    public final Guard<T> guard;
    
    private
    Variable(final T value, final Guard<T> guard) {
        this.value = value;
        this.guard = guard;
    }

    /**
     * Constructs a new variable.
     * @param <T>   value type   
     * @param value initial {@linkplain #get value}
     */
    static public <T> Variable<T>
    var(final T value) { return new Variable<T>(value, null); }
    
    /**
     * Constructs a new variable.
     * @param <T>   value type   
     * @param value initial {@linkplain #get value}
     * @param guard {@link #guard}
     */
    static public <T> Variable<T>
    var(final T value, final Guard<T> guard) {
        return new Variable<T>(guard.run(value), guard);
    }
    
    /**
     * Assigns the variable.
     * @param value new value
     */
    public void
    run(final T value) {this.value = null != guard ? guard.run(value) : value;}
    
    /**
     * Gets the current value.
     */
    public T
    get() { return value; }
}
