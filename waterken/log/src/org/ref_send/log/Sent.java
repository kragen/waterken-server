// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs a message send.
 * @see Got
 */
public class
Sent extends Event {
    static private final long serialVersionUID = 1L;
    
    /**
     * globally unique identifier for the message
     */
    public final String message;
    
    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param timestamp {@link #timestamp}
     * @param trace     {@link #trace}
     * @param message   {@link #message}
     */
    public @deserializer
    Sent(@name("anchor") final Anchor anchor,
         @name("timestamp") final Long timestamp,
         @name("trace") final Trace trace,
         @name("message") final String message) {
        super(anchor, timestamp, trace);
        this.message = message;
    }
}
