// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

/**
 * An event loop.
 * @param <T> {@link Task} type to execute
 */
public interface
Loop<T extends Task> {

    /**
     * Posts a task to this event loop.
     * @param task  task to execute
     */
    void
    run(T task);
}
