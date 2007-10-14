// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A record containing a field of each type.
 */
public final class
AllTypes extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * a boolean
     */
    public final boolean b;
    
    /**
     * a byte
     */
    public final byte o;
    
    /**
     * a char
     */
    public final char c;
    
    /**
     * a double
     */
    public final double d;
    
    /**
     * a float
     */
    public final float f;
    
    /**
     * an int
     */
    public final int i;
    
    /**
     * a long
     */
    public final long l;
    
    /**
     * a short
     */
    public final short s;
    
    /**
     * a string
     */
    public final String t;
    
    /**
     * a pass-by-reference
     */
    public final Runnable r;
    
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
     * @param t {@link #t}
     * @param r {@link #r}
     */
    public @deserializer
    AllTypes(@name("b") final boolean b,
             @name("o") final byte o,
             @name("c") final char c,
             @name("d") final double d,
             @name("f") final float f,
             @name("i") final int i,
             @name("l") final long l,
             @name("s") final short s,
             @name("t") final String t,
             @name("r") final Runnable r) {
        this.b = b;
        this.o = o;
        this.c = c;
        this.d = d;
        this.f = f;
        this.i = i;
        this.l = l;
        this.s = s;
        this.t = t;
        this.r = r;
    }
}
