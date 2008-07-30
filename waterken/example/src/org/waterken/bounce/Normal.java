// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.ref_send.promise.eventual.Receiver;

/**
 * A normal pass-by-reference object.
 */
class
Normal extends Struct implements Receiver<Void>, Powerless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public
    Normal() {}
    
    // java.lang.Runnable interface
    
    /**
     * Does nothing.
     */
    public void
    run(final Void ignored) {}
}
