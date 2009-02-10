// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bang;

import static org.ref_send.promise.eventual.Eventual.ref;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Promise;

/**
 * 
 */
public final class
DrumFactory extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    private
    DrumFactory() {}

    static public Promise<DrumFactory>
    make() { return ref(new DrumFactory()); }
    
    public Drum
    makeDrum() { return Bang.make(); }
}
