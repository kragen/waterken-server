// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs {@linkplain org.ref_send.promise.Resolver#reject rejection} of a
 * promise.
 */
public class
Rejected extends Resolved {
    static private final long serialVersionUID = 1L;

    /**
     * reason for rejecting the promise
     */
    public final Exception reason;
    
    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param timestamp {@link #timestamp}
     * @param trace     {@link #trace}
     * @param condition {@link #condition}
     * @param reason    {@link #reason}
     */
    public @deserializer
    Rejected(@name("anchor") final Anchor anchor,
             @name("timestamp") final Long timestamp,
             @name("trace") final Trace trace,
             @name("condition") final String condition,
             @name("reason") final Exception reason) {
        super(anchor, timestamp, trace, condition);
        this.reason = reason;
    }
}
