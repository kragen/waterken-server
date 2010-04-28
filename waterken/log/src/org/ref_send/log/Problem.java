// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs an uncaught exception.
 */
public class
Problem extends Comment {
    static private final long serialVersionUID = 1L;

    /**
     * uncaught exception
     */
    public final Exception reason;
    
    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param timestamp {@link #timestamp}
     * @param trace     {@link #trace}
     * @param text      {@link #text}
     * @param reason    {@link #reason}
     */
    public @deserializer
    Problem(@name("anchor") final Anchor anchor,
            @name("timestamp") final Long timestamp,
            @name("trace") final Trace trace,
            @name("text") final String text,
            @name("reason") final Exception reason) {
        super(anchor, timestamp, trace, text);
        this.reason = reason;
    }
}
