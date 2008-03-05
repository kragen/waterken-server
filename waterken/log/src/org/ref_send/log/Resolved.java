// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs resolution of a condition.
 */
public class
Resolved extends Entry {
    static private final long serialVersionUID = 1L;

    /**
     * globally unique identifier for the condition
     */
    public final String condition;
    
    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param trace     {@link #trace}
     * @param condition {@link #condition}
     */
    public @deserializer
    Resolved(@name("anchor") final Anchor anchor,
             @name("trace") final Trace trace,
             @name("condition") final String condition) {
        super(anchor, trace);
        this.condition = condition;
    }
}
