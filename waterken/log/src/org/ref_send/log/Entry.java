// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A log entry.
 */
public class
Entry extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * event loop turn in which the event occured
     */
    public final Turn turn;
    
    /**
     * trace of the call site that produced the event
     */
    public final Trace trace;
    
    /**
     * Constructs an instance.
     * @param turn  {@link #turn}
     * @param trace {@link #trace}
     */
    public @deserializer
    Entry(@name("turn") final Turn turn,
          @name("trace") final Trace trace) {
        this.turn = turn;
        this.trace = trace;
    }
}
