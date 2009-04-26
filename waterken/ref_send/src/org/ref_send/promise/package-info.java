// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html

/**
 * A promise interface.
 * <p>The ref_send API provides a language for expressing eventual control flow,
 * where operations are only scheduled to happen later, instead of being
 * executed immediately, as is the case with the normal flow of control in Java.
 * To support eventual control flow, the ref_send API uses a different kind of
 * reference, called a promise. A promise is a reference to an object which has
 * yet to be determined. It's this flexibility that enables coding of an
 * algorithm that manipulates values which won't be calculated until later, as
 * is done in eventual control flow.</p>
 * <p>One way to think about promises is as a generalization of floating point
 * numbers. Floating point arithmetic has a special way of dealing with error
 * conditions, different from that used in integer arithmetic. For example, the
 * expression "<code>0 / 0</code>" will throw an
 * {@link java.lang.ArithmeticException}, which aborts the current flow of
 * control.  In contrast, the expression "<code>0.0 / 0.0</code>" does not throw
 * an exception, instead returning a special value called a <code>NaN</code> and
 * allowing the current flow of control to continue.</p>
 * <p>Like a floating point number, a promise can represent either a normal
 * value or an error condition. A fulfilled promise is a wrapper containing a
 * normal Java reference. A rejected promise is a wrapper containing an
 * {@link java.lang.Exception} specifying the details of the error condition.
 * For example:</p>
 * <pre>
 * import static org.ref_send.promise.Eventual.ref;
 * &hellip;
 *
 *     private int balance;
 *     &hellip;
 *
 *     public Promise&lt;Integer&gt;
 *     getBalance() { return ref(balance); }
 *     &hellip;
 * </pre>
 * <p>The static {@link org.ref_send.promise.Eventual#ref ref}()
 * function takes a normal Java reference and returns a corresponding
 * {@link org.ref_send.promise.Promise}.</p>
 * <p>In floating point arithmetic, the <code>NaN</code> value is contagious,
 * meaning that any other expression that uses it also returns <code>NaN</code>.
 * For example, the expression "<code>0.0 / 0.0 + 1.0</code>" also returns
 * <code>NaN</code>. An algorithm that uses floating point numbers can thus be
 * coded such that it always runs to completion and the error condition is
 * propagated through to the return value. A rejected promise can be used in a
 * similar way. For example:</p>
 * <pre>
 * public interface
 * Account {
 *     public Promise&lt;Integer&gt;
 *     getBalance();
 * }
 *
 * public class
 * Customer {
 *     &hellip;
 *
 *     public Account
 *     getSavings() {
 *         if (frozen) { throw new Frozen(); }
 *         return savings;
 *     }
 * }
 *
 * &hellip;
 *     final Eventual _ = &hellip;
 *     final Customer client_ = _._(client);
 *     final Promise&lt;Integer&gt; current = client_.getSavings().getBalance();
 *     &hellip;
 * </pre>
 * <p>In the above code, the <code>current</code> balance will be a rejected
 * promise, with reason <code>Frozen</code>, if the customer's savings account
 * has been frozen by the bank. The rejected promise was originally produced by
 * the <code>getSavings()</code> call, but propagated through the
 * <code>getBalance()</code> invocation to the <code>current</code> balance.</p>
 * <p>In addition to representing a normal or error condition, a promise is most
 * useful for representing a value which is yet to be determined. Such a promise
 * may be used to refer to a value which will be calculated later, based on
 * inputs which are not yet known. The
 * {@link org.ref_send.promise.Eventual} class supports creating this
 * kind of
 * {@linkplain org.ref_send.promise.Eventual#defer unresolved promise},
 * as well as doing
 * {@linkplain org.ref_send.promise.Eventual#when conditional}
 * operations on promises.</p>
 */
@org.joe_e.IsJoeE package org.ref_send.promise;
