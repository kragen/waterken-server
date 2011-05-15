// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.pipelined;

import java.io.Serializable;

/**
 * A {@link PlugNPlay} maker.
 */
public final class PlugNPlayMaker {
    private PlugNPlayMaker() {}
    
    static public PlugNPlay
    make() {
        class PlugNPlayX implements PlugNPlay, Serializable {
            static private final long serialVersionUID = 1L;
            
            private PlugNPlay player = this;

            public PlugNPlay
            play() { return player; }

            public PlugNPlayX
            plug(final PlugNPlay player) {
              this.player = player;
              return (PlugNPlayX)player;
            }
        }
        return new PlugNPlayX();
    }
}
