// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs sending of a return value.
 */
public class
Returned extends Sent {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param trace     {@link #trace}
     * @param message   {@link #message}
     */
    public @deserializer
    Returned(@name("anchor") final Anchor anchor,
             @name("trace") final Trace trace,
             @name("message") final String message) {
        super(anchor, trace, message);
    }
}
