// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

/**
 * Schedules delayed {@link Effect}s.
 */
public interface
Scheduler<S> {

    /**
     * Schedules a delayed effect.
     * @param delay     number of milliseconds to wait before executing
     * @param effect    effect to execute
     */
    void apply(long delay, Effect<S> effect);
}
