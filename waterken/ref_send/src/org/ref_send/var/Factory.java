// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

import org.joe_e.Struct;

/**
 * An object factory.
 */
public abstract class
Factory<T> extends Struct {

    protected
    Factory() {}
    
    /**
     * Produces an object.
     */
    public abstract T
    run();
}
