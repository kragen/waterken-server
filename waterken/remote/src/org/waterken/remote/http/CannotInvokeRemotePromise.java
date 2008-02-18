// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import org.ref_send.deserializer;
import org.ref_send.promise.eventual.Eventual;
import org.web_send.Failure;

/**
 * Signals an attempted eventual invocation on a remote promise.
 * <p>
 * To ensure invocations are delivered in the same order as enqueued, only send
 * an invocation on a remote reference, or a pipeline promise generated from
 * this vat. Client code should do a {@link Eventual#when} operation on a
 * remote promise before sending invocations.
 * </p>
 */
public class
CannotInvokeRemotePromise extends Failure {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    CannotInvokeRemotePromise() {
        super("404", "never");
    }
}
