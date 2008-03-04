// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * An event identifier.
 */
public class
Event implements Comparable<Event>, Selfless, Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * event loop turn in which the event occured
     */
    public final Turn turn;
    
    /**
     * intra-{@linkplain #turn turn} event number
     */
    public final long number;
    
    /**
     * Constructs an instance.
     * @param turn      {@link #turn}
     * @param number    {@link #number}
     */
    public @deserializer
    Event(@name("turn") final Turn turn,
          @name("number") final long number) {
        this.turn = turn;
        this.number = number;
    }
    
    // org.joe_e.Selfless interface
    
    public boolean
    equals(final Object o) {
        return o instanceof Event &&
            number == ((Event)o).number &&
            (null!=turn ? turn.equals(((Event)o).turn) : null==((Event)o).turn);
    }
    
    public int
    hashCode() {
        return (null != turn ? turn.hashCode() : 0) +
               (int)(number ^ (number >>> 32)) +
               0x10097C42;
    }

    // java.lang.Comparable interface
    
    public int
    compareTo(final Event o) {
        final int major = turn.compareTo(o.turn);
        if (0 != major) { return major; }
        final long minor = number - o.number;
        return minor < 0L ? -1 : minor == 0L ? 0 : 1;
    }
}
