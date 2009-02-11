// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.BooleanArray;
import org.joe_e.array.ByteArray;
import org.joe_e.array.CharArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.DoubleArray;
import org.joe_e.array.FloatArray;
import org.joe_e.array.IntArray;
import org.joe_e.array.LongArray;
import org.joe_e.array.ShortArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Receiver;
import org.ref_send.promise.Volatile;

/**
 * A record containing a field of each type.
 */
public final class
AllTypes extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * boolean
     */
    public final BooleanArray b;
    
    /**
     * char
     */
    public final CharArray c; 
   
    /**
     * float
     */
    public final FloatArray f;
 
    /**
     * double
     */
    public final DoubleArray d;
     
    /**
     * byte
     */
    public final ByteArray o;
    
    /**
     * short
     */
    public final ShortArray s;
    
    /**
     * int
     */
    public final IntArray i;
    
    /**
     * long
     */
    public final LongArray l;
    
    /**
     * string
     */
    public final String t;
    
    /**
     * pass-by-reference
     */
    public final ConstArray<Receiver<?>> r;
    
    /**
     * promise
     */
    public final ConstArray<? extends Volatile<?>> p;
    
    /**
     * unknown type
     */
    public final Object a;
    
    /**
     * Constructs an instance.
     * @param b {@link #b}
     * @param o {@link #o}
     * @param c {@link #c}
     * @param d {@link #d}
     * @param f {@link #f}
     * @param i {@link #i}
     * @param l {@link #l}
     * @param s {@link #s}
     * @param a {@link #a}
     * @param t {@link #t}
     * @param r {@link #r}
     */
    public @deserializer
    AllTypes(@name("b") final BooleanArray b,
             @name("c") final CharArray c,
             @name("f") final FloatArray f,
             @name("d") final DoubleArray d,
             @name("o") final ByteArray o,
             @name("s") final ShortArray s,
             @name("i") final IntArray i,
             @name("l") final LongArray l,
             @name("t") final String t,
             @name("r") final ConstArray<Receiver<?>> r,
             @name("p") final ConstArray<? extends Volatile<?>> p,
             @name("a") final Object a) {
        this.b = b;
        this.c = c;
        this.f = f;
        this.d = d;
        this.o = o;
        this.s = s;
        this.i = i;
        this.l = l;
        this.t = t;
        this.r = r;
        this.p = p;
        this.a = a;
    }
}
