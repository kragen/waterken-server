// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bang;

import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;

import org.ref_send.promise.Promise;

/**
 * A {@link Drum} maker.
 */
public final class
Bang {
    private Bang() {}
    
    /**
     * Constructs a {@link Drum}.
     */
    static public Drum
    make() {
        class DrumX implements Drum, Serializable {
            static private final long serialVersionUID = 1L;
            
            private int hits = 0;

            public Promise<Integer>
            getHits() { return ref(hits); }
            
            public Drum
            bang(final int beats) {
                if (0 > beats) { throw new RuntimeException(); }
                hits += beats;
                return this;
            }
        }
        return new DrumX();
    }
}
