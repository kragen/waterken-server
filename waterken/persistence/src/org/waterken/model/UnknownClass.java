// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals a {@link ClassNotFoundException} during deserialization.
 */
public class
UnknownClass extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * detail message
     */
    public final String message;
    
    /**
     * Constructs an instance.
     * @param message   {@link #message}
     */
    public @deserializer
    UnknownClass(@name("message") final String message) {
        super(message);
        this.message = message;
    }
}
