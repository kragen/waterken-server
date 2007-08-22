// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Selfless;

/**
 * A promise that alleges to be fulfilled.
 * @param <T> referent type
 */
public class
Fulfilled<T> implements Promise<T>, Selfless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * referent
     */
    private final T value;

    /**
     * Construct an instance.
     * @param value {@link #cast value}
     */
    protected
    Fulfilled(final T value) {
        this.value = value;
    }

    /**
     * Adapts an immediate reference to the {@link Promise} interface.
     * @param <T> referent type
     * @param value referent
     * @return promise that {@linkplain #cast refers} to the <code>value</code>,
     *         or <code>null</code> if <code>value</code> is <code>null</code>
     */
    static public <T> Promise<T>
    ref(final T value) { return null != value ? new Inline<T>(value) : null; }

    /**
     * Marks a point where deserialization of an object graph may be deferred.
     * <p>
     * If a referrer holds the promise returned by this method, instead of a
     * direct reference to the referent, the persistence engine may defer
     * deserialization of the referent until it is {@linkplain #cast accessed}.
     * </p>
     * @param <T> referent type
     * @param value referent
     * @return promise that {@linkplain #cast refers} to the <code>value</code>
     */
    static public <T> Fulfilled<T>
    detach(final T value) { return new Fulfilled<T>(value); }

    // java.lang.Object interface

    /**
     * Is the given object the same?
     * @param x compared to object
     * @return <code>true</code> if the same, else <code>false</code>
     */
    public boolean
    equals(final Object x) {
        return x instanceof Fulfilled &&
               same(cast(), ((Fulfilled)x).cast());
    }

    static private boolean
    same(final Object a, final Object b) {
        return null != a ? a.equals(b) : null == b;
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0xF0F111ED; }

    // org.ref_send.promise.Volatile interface

    /**
     * Gets the fulfilled value.
     */
    public T
    cast() { return value; }
}
