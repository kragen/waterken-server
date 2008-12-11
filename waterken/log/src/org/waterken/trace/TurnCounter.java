// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.log.Anchor;
import org.ref_send.log.Turn;
import org.ref_send.promise.eventual.Task;

/**
 * An event loop turn counter.
 */
public class
TurnCounter extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * URI for the event loop
     */
    public final String loop;

    /**
     * increment the turn counter
     */
    public final Task<Turn> flip;
    
    /**
     * increment the anchor counter
     */
    public final Marker mark;
    
    private
    TurnCounter(final String loop,
                final Task<Turn> flip,
                final Marker mark) {
        this.loop = loop;
        this.flip = flip;
        this.mark = mark;
    }
    
    /**
     * Constructs an instance.
     * @param loop  {@link #loop}
     */
    static public TurnCounter
    make(final String loop) {
        class State implements Serializable {
            static private final long serialVersionUID = 1L;

            long turns = 0;     // id of current turn
            long anchors = 0;   // id of next anchor
        }
        final State m = new State();
        class Flip extends Struct implements Task<Turn>, Serializable {
            static private final long serialVersionUID = 1L;

            public Turn
            run() { 
                m.anchors = 0;
                return new Turn(loop, ++m.turns);
            }
        }
        class Mark extends Struct implements Marker, Serializable {
            static private final long serialVersionUID = 1L;

            public Anchor
            run() { return new Anchor(new Turn(loop, m.turns), m.anchors++); }
        }
        return new TurnCounter(loop, new Flip(), new Mark());
    }
}
