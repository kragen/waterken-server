// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs a comment.
 */
public class
Comment extends Event {
    static private final long serialVersionUID = 1L;

    /**
     * comment text
     */
    public final String text;
    
    /**
     * Constructs an instance.
     * @param turn  {@link #turn}
     * @param trace {@link #trace}
     * @param text  {@link #text}
     */
    public @deserializer
    Comment(@name("turn") final Turn turn,
            @name("trace") final Trace trace,
            @name("text") final String text) {
        super(turn, trace);
        this.text = text;
    }
}
