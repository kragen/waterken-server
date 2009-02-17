// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.Record;

/**
 * A DNS resource record.
 */
public class
Resource extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * RR type: host address
     */
    static public final short A = 1;
    
    /**
     * RR class: Internet
     */
    static public final short IN = 1;
    
    /**
     * Constructs a resource.
     * @param type  {@link #type}
     * @param clazz {@link #clazz}
     * @param ttl   {@link #ttl}
     * @param data  {@link #data}
     */
    static public ByteArray
    rr(final short type, final short clazz, final int ttl, final byte... data) {
        final ByteArray.Builder r = ByteArray.builder(2+2+4+2+data.length);
        r.append((byte)(type >>> 8));
        r.append((byte)(type      ));
        r.append((byte)(clazz >>> 8));
        r.append((byte)(clazz      ));
        r.append((byte)(ttl >>> 24));
        r.append((byte)(ttl >>> 16));
        r.append((byte)(ttl >>>  8));
        r.append((byte)(ttl       ));
        r.append((byte)(data.length >>>  8));
        r.append((byte)(data.length       ));
        r.append(data);
        return r.snapshot();
    }
    
    /**
     * RR type code, such as {@link #A}
     */
    static public short
    type(final ByteArray x) {return (short)(x.getByte(0) << 8 | x.getByte(1));}
    
    /**
     * class of {@link #data}, such as {@link #IN}
     */
    static public short
    clazz(final ByteArray x) {return (short)(x.getByte(2) << 8 | x.getByte(3));}
    
    /**
     * unsigned time-to-live in seconds
     */
    static public int
    ttl(final ByteArray x) {
        return x.getByte(4) << 24 | x.getByte(5) << 16 |
               x.getByte(6) <<  8 | x.getByte(7)       ;
    }
    
    /**
     * {@linkplain #data resource description} length
     */
    static public short
    length(final ByteArray x){return (short)(x.getByte(8) << 8 | x.getByte(9));}
    
    /**
     * resource description
     */
    static public ByteArray
    data(final ByteArray x) {
        final byte[] r = new byte[length(x)];
        System.arraycopy(r, 0, x.toByteArray(), 10, r.length);
        return ByteArray.array(r);
    }
}
