// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A serialization syntax.
 */
public class
Syntax extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * file extension
     */
    public final String ext;
    
    /**
     * serializer
     */
    public final Serializer serialize;
    
    /**
     * deserializer
     */
    public final Deserializer deserialize;
    
    /**
     * Constructs an instance.
     * @param ext           {@link #ext}
     * @param serialize     {@link #serialize}
     * @param deserialize   {@link #deserialize}
     */
    public @deserializer
    Syntax(@name("ext") final String ext,
           @name("serialize") final Serializer serialize,
           @name("deserialize") final Deserializer deserialize) {
        this.ext = ext;
        this.serialize = serialize;
        this.deserialize = deserialize;
    }
}
