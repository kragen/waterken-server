// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Volatile;

/**
 * A {@link Promise} resolver.
 * @param <T> promised referent type
 */
public interface
Resolver<T> extends Receiver<T> {

    /**
     * Put the corresponding promise in the fulfilled state.
     * <p>
     * This method is syntactic sugar for:
     * </p>
     * <pre>
     *     resolve(Eventual.promised(value));
     * </pre>
     * @param referent  fulfilled value of the corresponding promise
     */
    void
    run(T referent);

    /**
     * Put the corresponding promise in the rejected state.
     * <p>
     * This method is syntactic sugar for:
     * </p>
     * <pre>
     *     resolve(new Rejected&lt;T&gt;(reason));
     * </pre>
     * @param reason    reason the corresponding promise will not be fulfilled
     */
    Void
    reject(Exception reason);
    
    /**
     * Chains the correponding promise to the given promise.
     * @param promise   promise to forward requests to
     */
    void
    resolve(Volatile<T> promise);
}
