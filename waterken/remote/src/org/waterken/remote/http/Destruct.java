// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.vat.Root;

/**
 * An exportable reference to the destruct operation.
 */
public final class
Destruct extends Struct implements Receiver<Object>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * {@link Root#destruct}
     */
    private final Receiver<?> destruct;
    
    Destruct(final Receiver<?> destruct) {
        this.destruct = destruct;
    }
    
    // java.lang.Runnable interface

    public void
    run(final Object value) { destruct.run(null); }
}
