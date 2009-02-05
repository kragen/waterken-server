// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Information about a messaging session.
 */
public class
SessionInfo extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * secret key for messaging
     */
    public final String key;
    
    /**
     * GUID for session logging
     */
    public final String name;
    
    /**
     * Constructs an instance.
     * @param key   {@link #key}
     * @param name  {@link #name}
     */
    public @deserializer
    SessionInfo(@name("key") final String key,
                @name("name") final String name) {
        this.key = key;
        this.name = name;
    }
}
