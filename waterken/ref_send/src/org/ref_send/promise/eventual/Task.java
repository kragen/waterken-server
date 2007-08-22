// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

/**
 * An {@linkplain Loop event loop} task.
 */
public interface
Task {

    /**
     * Executes the task.
     * @throws Exception    any problem
     */
    void
    run() throws Exception;
}
