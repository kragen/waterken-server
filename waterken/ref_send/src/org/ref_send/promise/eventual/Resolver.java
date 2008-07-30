// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;

/**
 * A {@link Promise} resolver.
 * @param <T> promised referent type
 */
public interface
Resolver<T> extends Receiver<T> {

    /**
     * Resolve the corresponding promise to the given reference.
     * <p>
     * This method is syntactic sugar for:
     * </p>
     * <pre>
     * {@link #resolve resolve}({@link Eventual#promised promised}(referent));
     * </pre>
     * @param referent  resolved value of the corresponding promise
     */
    void
    run(T referent);

    /**
     * Put the corresponding promise in the rejected state.
     * <p>
     * This method is syntactic sugar for:
     * </p>
     * <pre>
     * {@link #resolve resolve}(new {@link Rejected}&lt;T&gt;(reason));
     * </pre>
     * @param reason    reason the corresponding promise will not be fulfilled
     */
    Void
    reject(Exception reason);
    
    /**
     * Resolve the corresponding promise to the given promise.
     * @param promise   promise to forward requests to
     */
    void
    resolve(Volatile<T> promise);
}
