// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs receipt of a message.
 */
public class
Got extends Entry {
    static private final long serialVersionUID = 1L;
    
    /**
     * globally unique identifier for the message
     */
    public final String message;
    
    /**
     * Constructs an instance.
     * @param event     {@link #event}
     * @param trace     {@link #trace}
     * @param message   {@link #message}
     */
    public @deserializer
    Got(@name("event") final Event event,
        @name("trace") final Trace trace,
        @name("message") final String message) {
        super(event, trace);
        this.message = message;
    }
}
