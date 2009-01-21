// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.eq;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Receiver;

/**
 * A normal pass-by-reference object.
 */
final class
Normal extends Struct implements Receiver<Object>, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Does nothing.
     */
    public void
    run(final Object ignored) {}
}
