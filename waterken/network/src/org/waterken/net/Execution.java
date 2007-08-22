// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net;

/**
 * The authority to delay execution in the current thread.
 */
public interface
Execution {

    /**
     * Sleeps the current thread.
     * @param ms    number of milliseconds to sleep for
     */
    void
    sleep(long ms) throws InterruptedException;
    
    /**
     * Allow other threads to execute.
     */
    void
    yield();
}
