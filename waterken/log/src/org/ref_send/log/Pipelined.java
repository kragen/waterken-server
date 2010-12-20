// Copyright 2010 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html

package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs a conditional send of a message processed at the callee's site. 
 */
public class
Pipelined extends SentIf {
	static private final long serialVersionUID = 1L;

	/**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param timestamp {@link #timestamp}
     * @param trace     {@link #trace}
     * @param message   {@link #message}
     * @param condition {@link #condition}
     */
    public @deserializer
    Pipelined(@name("anchor") final Anchor anchor,
    		  @name("timestamp") final Long timestamp,
    		  @name("trace") final Trace trace,
    		  @name("message") final String message,
    		  @name("condition") final String condition) {
		super(anchor, timestamp, trace, message, condition);
    }
}
