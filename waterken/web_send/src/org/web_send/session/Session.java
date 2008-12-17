// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.session;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Configuration for a persistent messaging session.
 */
public class
Session extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * key bound to the session maker in all vats
     */
    static public final String makerKey = "sessions";
    
    /**
     * GUID for this session
     */
    public final String name;
    
    /**
     * secret key for using this session
     */
    public final String key;
    
    /**
     * Constructs an instance.
     * @param name  {@link #name}
     * @param key   {@link #key}
     */
    public @deserializer
    Session(@name("name") final String name,
            @name("key") final String key) {
        this.name = name;
        this.key = key;
    }
}
