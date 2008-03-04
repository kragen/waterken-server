// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs a comment.
 */
public class
Comment extends Entry {
    static private final long serialVersionUID = 1L;

    /**
     * comment text
     */
    public final String text;
    
    /**
     * Constructs an instance.
     * @param event {@link #event}
     * @param trace {@link #trace}
     * @param text  {@link #text}
     */
    public @deserializer
    Comment(@name("event") final Event event,
            @name("trace") final Trace trace,
            @name("text") final String text) {
        super(event, trace);
        this.text = text;
    }
}
