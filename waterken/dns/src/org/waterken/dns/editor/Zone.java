// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Description of hostnames that may be {@linkplain Registrar#claim claimed}.
 */
public class
Zone extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * required suffix on a hostname
     */
    public final String suffix;

    /**
     * required prefix on a hostname
     */
    public final String prefix;
    
    /**
     * Constructs an instance.
     * @param suffix    {@link #suffix}
     * @param prefix    {@link #prefix}
     */
    public @deserializer
    Zone(@name("suffix") final String suffix,
         @name("prefix") final String prefix) {
        this.suffix = suffix;
        this.prefix = prefix;
    }
}
