// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.delayed;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Deferred;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;

/**
 * A {@link Forwarder} maker.
 */
public final class
ForwarderMaker {
    private ForwarderMaker() {}
    
    static public Forwarder
    make(final Eventual _) {
        class ForwarderX extends Struct implements Forwarder, Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Deferred<Boolean>>
            forward() {
                final Deferred<Boolean> r = _.defer();
                return Eventual.ref(r);
            }
        }
        return new ForwarderX();
    }
}
