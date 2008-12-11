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
     * The <code>builder</code> MUST have a method with signature:
     * </p>
     * <pre>
     * static public R
     * build({@link Framework} framework<i>, &hellip;</i>)
     * </pre>
     * @param <R> return type, MUST be either an interface, or a {@link Promise}
     * @param builder   object constructor class
     * @param argv      more arguments for <code>builder</code>'s build method
     * @return promise for the object returned by the <code>builder</code>
     */
    public abstract <R> R
    run(Class<?> builder, Object... argv);
}
