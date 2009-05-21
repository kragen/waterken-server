// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bang;

import java.io.Serializable;

import org.joe_e.Struct;

/**
 * A {@link Drum} factory.
 */
public final class
DrumFactory extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    private
    DrumFactory() {}

    /**
     * Constructs an instance.
     */
    static public DrumFactory
    make() { return new DrumFactory(); }
    
    /**
     * Constructs a drum.
     */
    public Drum
    makeDrum() { return Bang.make(); }
}
