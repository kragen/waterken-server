// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.pipelined;

/**
 * A pipeline factory and tester.
 */
public interface PlugNPlay {

    /**
     * Creates a promise pipeline.
     */
    PlugNPlay play();

    /**
     * Receives a promise pipeline.
     * @param player    promise pipeline
     */
    PlugNPlay plug(PlugNPlay player);
}
