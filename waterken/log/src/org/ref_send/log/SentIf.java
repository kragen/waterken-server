// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Logs a conditional message send.
 * <p>
 * This kind of event is produced for a
 * {@linkplain org.ref_send.promise.eventual.Eventual#when when block}. The
 * {@link #message} identifies the when block itself, and the {@link #condition}
 * identifies the promise the when block is queued on.
 * </p>
 */
public class
SentIf extends Sent {
    static private final long serialVersionUID = 1L;

    /**
     * globally unique identifier for the condition
     */
    public final String condition;

    /**
     * Constructs an instance.
     * @param anchor    {@link #anchor}
     * @param trace     {@link #trace}
     * @param message   {@link #message}
     * @param condition {@link #condition}
     */
    public @deserializer
    SentIf(@name("anchor") final Anchor anchor,
           @name("trace") final Trace trace,
           @name("message") final String message,
           @name("condition") final String condition) {
        super(anchor, trace, message);
        this.condition = condition;
    }
}
