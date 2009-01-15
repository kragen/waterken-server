// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

import org.joe_e.Struct;
import org.ref_send.promise.Promise;

/**
 * A vat maker.
 * @see org.web_send.graph.Publisher#spawn
 */
public abstract class
Spawn extends Struct {
    
    /**
     * Creates a vat.
     * <p>
     * The <code>maker</code> MUST have a method with signature:
     * </p>
     * <pre>
     * static public R
     * make({@link Eventual} _, {@link Vat} vat<i>, &hellip;</i>)
     * </pre>
     * <p>
     * All of the parameters in the make method are optional, but MUST appear
     * in the order shown if present.
     * </p>
     * @param <R> return type, MUST be either an interface, or a {@link Promise}
     * @param maker object constructor class
     * @param argv  more arguments for <code>makers</code>'s make method
     * @return promise for the object returned by the <code>make</code>
     */
    public abstract <R> R
    run(Class<?> maker, Object... argv);
}
