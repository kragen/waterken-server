// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A DNS question
 *
 */
public class
Question extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * any {@link #type}
     */
    static public final short anyType = 255;
    
    /**
     * any {@link #clazz}
     */
    static public final short anyClass = 255;
    
    /**
     * hostname
     */
    public final String name;
    
    /**
     * RR type code
     */
    public final short type;
    
    /**
     * RR class
     */
    public final short clazz;
    
    /**
     * Constructs an instance.
     * @param name  {@link #name}
     * @param type  {@link #type}
     * @param clazz {@link #clazz}
     */
    public @deserializer
    Question(@name("name") final String name,
             @name("type") final short type,
             @name("clazz") final short clazz) {
        this.name = name;
        this.type = type;
        this.clazz = clazz;
    }
}
