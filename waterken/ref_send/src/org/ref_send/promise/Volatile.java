// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

/**
 * A promise of unknown origin.
 * <p>A promise held in a variable of type {@link Volatile} must not be trusted
 * to implement the semantics of a {@link Promise}. In particular, the client
 * code must be prepared to correctly handle an implementation that changes its
 * referent, or reports a referent and then later reports failure to acquire a
 * referent, or switches back and forth between all of these states.
 * </p>
 * @param <T> referent type
 */
public interface
Volatile<T> {
    
    /**
     * Gets the current referent.
     * <p>
     * For example:
     * </p>
     * <pre>
     * final Volatile&lt;Foo&gt; foo = &hellip;
     * try {
     *     foo.cast().bar();
     * } catch (final Exception reason) {
     *     // Either there is no current referent, or the bar() call failed.
     *     throw reason;
     * }
     * </pre>
     * @throws Exception    reason the referent is not known
     */
    T
    cast() throws Exception;
}
