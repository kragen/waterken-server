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
     * minimum recommended {@link #ttl}
     */
    static public final int minTTL = 60;
    
    /**
     * localhost {@link #A} resource
     */
    static public final ByteArray localhost =
        rr(A, IN, minTTL, new byte[] { 127,0,0,1 });
    
    /**
     * number of bytes preceding the {@link #data}
     */
    static public final int headerLength = 2 + 2 + 4 + 2;
    
    /**
     * Constructs a resource.
     * @param type  {@link #type}
     * @param clazz {@link #clazz}
     * @param ttl   {@link #ttl}
     * @param data  {@link #data}
     */
    static public ByteArray
    rr(final short type, final short clazz, final int ttl, final byte... data) {
        if (data.length >= (1 << 16)) { throw new RuntimeException(); }
        
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
    type(final ByteArray x) {
        return (short)((0xFF & x.getByte(0)) << 8 |
                       (0xFF & x.getByte(1))       );
    }
    
    /**
     * class of {@link #data}, such as {@link #IN}
     */
    static public short
    clazz(final ByteArray x) {
        return (short)((0xFF & x.getByte(2)) << 8 |
                       (0xFF & x.getByte(3))       );
    }
    
    /**
     * unsigned time-to-live in seconds
     */
    static public int
    ttl(final ByteArray x) {
        return (0xFF & x.getByte(4)) << 24 |
               (0xFF & x.getByte(5)) << 16 |
               (0xFF & x.getByte(6)) <<  8 |
               (0xFF & x.getByte(7))       ;
    }
    
    /**
     * {@linkplain #data resource description} length
     */
    static public int
    length(final ByteArray x){
        return (0xFF & x.getByte(8)) << 8 |
               (0xFF & x.getByte(9))      ;
    }
    
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
