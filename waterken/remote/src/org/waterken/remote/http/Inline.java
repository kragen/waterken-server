// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Promise;

/**
 * A fulfilled remote reference.
 */
/* package */ final class
Inline<T> extends Struct implements Promise<T>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * referent
     */
    private final T referent;

    /**
     * Construct an instance.
     * @param referent  {@link #referent}
     */
    protected
    Inline(final T referent) {
        this.referent = referent;
    }

    // org.ref_send.promise.Promise interface

    public T
    call() { return referent; }
}
