// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

/**
 * A {@link Promise} resolver.
 * @param <T> promised referent type
 */
public interface
Resolver<T> extends Receiver<T> {
    
    /**
     * Logs progress towards resolution.
     */
    void progress();

    /**
     * Put the corresponding promise in the rejected state.
     * <p>
     * This method is syntactic sugar for:
     * </p>
     * <pre>
     * {@link #resolve resolve}({@link Eventual#reject reject}(reason));
     * </pre>
     * @param reason    reason the corresponding promise will not be fulfilled
     */
    void reject(Exception reason);
    
    /**
     * Resolve the corresponding promise to the given promise.
     * @param promise   promise to forward requests to
     */
    void resolve(Promise<? extends T> promise);

    /**
     * Resolve the corresponding promise to the given reference.
     * @param referent  reference to forward requests to
     */
    void apply(T referent);
}
