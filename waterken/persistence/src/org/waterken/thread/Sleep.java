// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.thread;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Receiver;

/**
 * Permission to sleep the current thread.
 */
public final class
Sleep extends Struct implements Receiver<Long>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public
    Sleep() {}

    public void
    run(final Long ms) {
        try { Thread.sleep(ms); } catch (final InterruptedException e) {}
    }
}
