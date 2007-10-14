// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.put;

import static org.ref_send.Slot.var;
import static org.ref_send.promise.Fulfilled.ref;

import org.ref_send.Variable;
import org.ref_send.promise.Volatile;
import org.web_send.graph.Framework;

/**
 * A var factory.
 */
public final class
Put {

    private
    Put() {}
    
    /**
     * Constructs an instance.
     * @param framework model framework
     */
    static public Variable<Volatile<Byte>>
    build(final Framework framework) {
        return make();
    }
    
    /**
     * Constructs an instance.
     */
    static public Variable<Volatile<Byte>>
    make() {
        final Volatile<Byte> zero = ref((byte)0);
        return var(zero);
    }
}
