// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

/**
 * Delayed execution.
 */
public interface
Scheduler<T extends Promise<?>> {

    /**
     * Executes a task after a timeout has elapsed.
     * @param timeout   minimum number of milliseconds to delay for
     * @param task      task to {@link Promise#call() execute} after timeout
     */
    void apply(long timeout, T task);
}
