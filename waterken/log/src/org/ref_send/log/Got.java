// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs receipt of a message.
 * @see Sent
 */
public class
Got extends Event {
    static private final long serialVersionUID = 1L;
    
    /**
     * globally unique identifier for the message
     */
    public final String message;
    
    /**
     * difference, measured in milliseconds, between the time the event was
     * received and midnight, January 1, 1970 UTC
     * <p>
     * Is <code>null</code> if the timestamp is unknown.
     * </p>
     */
    public final Long timestamp;
    
    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param trace     {@link #trace}
     * @param message   {@link #message}
     */
    public @deserializer
    Got(@name("anchor") final Anchor anchor,
        @name("trace") final Trace trace,
        @name("message") final String message,
        @name("timestamp") final Long timestamp) {
        super(anchor, trace);
        this.message = message;
        this.timestamp = timestamp;
    }
}
