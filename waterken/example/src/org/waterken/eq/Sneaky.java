// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.eq;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Promise;

/**
 * A possibly sneaky promise.
 */
/* package */ final class
Sneaky<T> extends Struct implements Promise<T>, Serializable {
    static private final long serialVersionUID = 1L;
    
    private final T referent;

    protected
    Sneaky(final T referent) {
        this.referent = referent;
    }
    
    public T
    call() { return referent; }
}
