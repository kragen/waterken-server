// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.brand;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A typed <code>long</code> value.
 * @param <T> {@link #unit} type
 */
public class
Amount<T> implements Comparable<Amount<T>>, Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * unit of {@link #value}
     */
    public final Brand<T> unit;
    
    /**
     * number of units
     */
    public final long value;
    
    /**
     * Constructs an instance.
     * @param unit  {@link #unit}
     * @param value {@link #value}
     */
    public @deserializer
    Amount(@name("unit") final Brand<T> unit,
           @name("value") final long value) {
        this.unit = unit;
        this.value = value;
    }
    
    // java.lang.Object interface
    
    public boolean
    equals(final Object o) {
        return null != o && Amount.class == o.getClass() &&
            value == ((Amount<?>)o).value &&
            Brand.equal(unit, ((Amount<?>)o).unit);
    }
    
    public int
    hashCode() { return (int)(value ^ (value >>> Integer.SIZE)) + 0x0FAB4A2D; }

    // java.lang.Comparable interface

    public int
    compareTo(final Amount<T> o) throws WrongBrand {
        Brand.require(unit, o.unit);
        return value < o.value ? -1 : value == o.value ? 0 : 1;
    }
}
