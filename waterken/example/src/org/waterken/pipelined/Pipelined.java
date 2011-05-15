// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.pipelined;

import static org.ref_send.promise.Eventual.ref;

import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Vat;
import org.ref_send.test.Logic;

/**
 * Promise pipelining test.
 */
public final class Pipelined {
    private Pipelined() {}
    
    static public Promise<?>
    make(final Eventual _) {
        final Vat<PlugNPlay> far = _.spawn("pipeline", PlugNPlayMaker.class);
        final Promise<?> a = ref(far.top.plug(far.top.play().play()));
        final PlugNPlay near = PlugNPlayMaker.make();
        far.top.plug(near);
        final Promise<?> b = ref(far.top.play().play().play().plug(near)); 
        return Logic.join(_, a, b);
    }
}
