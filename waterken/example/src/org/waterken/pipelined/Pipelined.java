// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.pipelined;

import static org.ref_send.promise.Eventual.ref;

import org.ref_send.promise.Promise;

/**
 * Promise pipelining test.
 */
public final class Pipelined {
    private Pipelined() {}
    
    static public Promise<?>
    make(final PlugNPlay player_) {
        return ref(player_.plug(player_.play().play()));
    }
}
