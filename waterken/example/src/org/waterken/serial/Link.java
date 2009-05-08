// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Promise;

/**
 * A resolved element of a series.
 * @param <T> {@link #value} type
 */
public class
Link<T> extends Struct implements Element<T>, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * element value
     */
    public final Promise<T> value;
    
    /**
     * next element
     */
    public final Element<T> next;
    
    /**
     * Constructs an instance.
     * @param value {@link #value}
     * @param next  {@link #next}
     */
    public @deserializer
    Link(@name("value") final Promise<T> value,
         @name("next") final Element<T> next) {
        this.value = value;
        this.next = next;
    }
    
    /**
     * Constructs an instance.
     * @param <T>   {@link #value} type
     * @param value {@link #value}
     * @param next  {@link #next}
     */
    static public <T> Element<T>
    link(final Promise<T> value, final Element<T> next) {
        return new Link<T>(value, next);
    }
    
    public Promise<T>
    getValue() { return value; }

    public Element<T>
    getNext() { return next; }
}
