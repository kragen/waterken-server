// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.put;

import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.var.Variable.var;

import org.ref_send.promise.Promise;
import org.ref_send.var.Variable;
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
     * @param framework vat permissions
     */
    static public Promise<Variable<Boolean>>
    build(final Framework framework) { return ref(make()); }
    
    /**
     * Constructs an instance.
     */
    static public Variable<Boolean>
    make() { return var(false); }
}
