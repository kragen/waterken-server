// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.thread;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Receiver;

/**
 * 
 */
public final class
Yield extends Struct implements Receiver<Void>, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs an instance.
     */
    public
    Yield() {}

    public void
    run(final Void arg) { Thread.yield(); }
}
