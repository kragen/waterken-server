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
 * There are three states for a promise: fulfilled, rejected and deferred. A
 * fulfilled promise is successfully bound to a referent, which can be either
 * local or remote. A rejected promise failed to acquire a referent, and carries
 * an {@link Exception} specifying the reason for the failure. A deferred
 * promise is in neither the success nor failure state. The inverse of deferred
 * is resolved, meaning the promise is in either the success or failure state.
 * </p>
 * <p>
 * A promise alleges it will transition only once from deferred to either
 * fulfilled or rejected. If static analysis of a program guarantees that a
 * promise was produced by code trusted to implement these semantics, the
 * promise should be held in a variable of type {@link Promise}; otherwise, the
 * variable should be of type {@link Volatile}. For example, the return from
 * the {@link Fulfilled#ref ref} function should be held in a variable of type
 * {@link Promise}; whereas a promise parameter in the declaration of a public
 * method should be of type {@link Volatile}.
 * </p>
 * @param <T> referent type
 */
public interface
Promise<T> extends Volatile<T> {}
