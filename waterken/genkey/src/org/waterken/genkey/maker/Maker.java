// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.genkey.maker;

import static org.ref_send.promise.Eventual.ref;

import org.ref_send.promise.Promise;

/**
 * 
 */
public final class
Maker {
    private Maker() {}
    
    static public Promise<Boolean>
    make() { return ref(true); }
}
