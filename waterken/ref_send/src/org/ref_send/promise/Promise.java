// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

/**
 * A promise for a referent.
 * <p>
 * If you conceptualize a reference as being like an arrow, where an invocation
 * is started at the tail end of the arrow and delivered to the object pointed
 * to by the head end of the arrow, then a promise is like the tail end of an
 * arrow which doesn't yet point to anything. The object referred to by a
 * promise can be determined later. Using a promise, an algorithm can refer to
 * an object which will be the result of future computation.
 * </p>
 * <pre>
 *                       reference
 *                       |
 *            referrer ----&gt; referent
 *                     |
 *                     promise end of the reference
 * </pre>
 * <p>
 * There are three states for a promise: fulfilled, rejected and unresolved. A
 * fulfilled promise is successfully bound to a referent, which can be either
 * local or remote. A rejected promise failed to acquire a referent, and carries
 * an {@link Exception} specifying the reason for the failure. An unresolved
 * promise is in neither the success nor failure state. A promise is resolved if
 * it is in either the success or failure state.
 * </p>
 * @param <T> referent type
 */
public interface
Promise<T> {
    
    /**
     * Gets the current referent.
     * <p>
     * For example:
     * </p>
     * <pre>
     * final Promise&lt;Foo&gt; foo = &hellip;
     * try {
     *     foo.call().bar();
     * } catch (final Exception reason) {
     *     // Either there is no referent, or the bar() invocation failed.
     *     throw reason;
     * }
     * </pre>
     * @throws Exception    reason the referent is not known
     */
    T call() throws Exception;
}
