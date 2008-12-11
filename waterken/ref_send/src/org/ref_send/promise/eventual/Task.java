// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

/**
 * A delayed task.
 */
public interface
Task<R> {

    /**
     * Executes the task.
     * @return  task output
     * @throws Exception    any problem
     */
    R
    run() throws Exception;
}
