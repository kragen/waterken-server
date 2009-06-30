// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace;

import java.io.PrintStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.log.Comment;
import org.ref_send.log.Event;
import org.ref_send.promise.Receiver;

/**
 * Prints {@linkplain Comment comments} to an output stream.
 */
public final class
Verbose extends Struct implements Receiver<Event>, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final PrintStream out;
    private final Receiver<Event> next;
    
    /**
     * Constructs an instance.
     * @param out   output stream
     * @param next  next event receiver
     */
    public @deserializer
    Verbose(@name("out") final PrintStream out,
            @name("next") final Receiver<Event> next) {
        this.out = out;
        this.next = next;
    }
    
    public void
    apply(final Event value) {
        if (value instanceof Comment) {
            out.println(value.anchor.turn.loop + ": " + ((Comment)value).text);
        }
        if (null != next) { next.apply(value); }
    }
}
