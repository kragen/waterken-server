// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.promise.Promise;

/**
 * A {@link Wall} implementation.
 */
public final class
Bounce {

    private
    Bounce() {}
    
    /**
     * Constructs an instance.
     */
    static public Wall
    make() {
        class WallX extends Struct implements Wall, Powerless, Serializable {
            static private final long serialVersionUID = 1L;

            public <A> Promise<A>
            bounce(final A a) { return ref(a); }
        }
        return new WallX();
    }
}
