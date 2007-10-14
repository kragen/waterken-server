// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bounce;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;

/**
 * A normal pass-by-reference object.
 */
public class
Normal extends Struct implements Runnable, Powerless, Serializable {
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
    run() {}
}
