// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.pipelined;

import java.io.Serializable;

import org.joe_e.Struct;

/**
 * A {@link PlugNPlay} maker.
 */
public final class PlugNPlayMaker {
    private PlugNPlayMaker() { /**/ }
    
    static public PlugNPlay
    make() {
        class PlugNPlayX extends Struct implements PlugNPlay, Serializable {
            static private final long serialVersionUID = 1L;

            public PlugNPlayX
            play() { return this; }

            public PlugNPlayX
            plug(final PlugNPlay player) { return (PlugNPlayX)player; }
        }
        return new PlugNPlayX();
    }
}
