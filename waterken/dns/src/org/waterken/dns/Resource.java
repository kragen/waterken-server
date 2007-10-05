// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Promise;

/**
 * A DNS resource record.
 */
public class
Resource extends Struct
         implements Promise<Resource>, Powerless, Record, Serializable {
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
     * RR type code
     */
    public final short type;
    
    /**
     * class of {@link #data}
     */
    public final short clazz;
    
    /**
     * unsigned time-to-live in seconds
     */
    public final int ttl;
    
    /**
     * resource description
     */
    public final ByteArray data;
    
    /**
     * Constructs an instance.
     * @param type  {@link #type}
     * @param clazz {@link #clazz}
     * @param ttl   {@link #ttl}
     * @param data  {@link #data}
     */
    public @deserializer
    Resource(@name("type") final short type,
             @name("clazz") final short clazz,
             @name("ttl") final int ttl,
             @name("data") final ByteArray data) {
        this.type = type;
        this.clazz = clazz;
        this.ttl = ttl;
        this.data = data;
    }
    
    // org.ref_send.promise.Promise interface
    
    /**
     * @return <code>this</code>
     */
    public Resource
    cast() { return this; }
}
