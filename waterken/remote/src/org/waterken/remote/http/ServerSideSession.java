// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Log;
import org.ref_send.promise.Promise;

/**
 * The server-side state associated with a messaging session.
 */
/* package */ final class
ServerSideSession implements Serializable {
    static private final long serialVersionUID = 1L;

    private final Log log_;
    private final String name;              // GUID of this session

    private long current;                   // current window number
    private ConstArray<Return> returns;     // returns in current window

    static private final class
    Return implements Serializable {
        static private final long serialVersionUID = 1L;

        final int message;
        final Object value;

        Return(final int message, final Object value) {
            this.message = message;
            this.value = value;
        }
    }

    protected
    ServerSideSession(final Log log_, final String name) {
        this.log_ = log_;
        this.name = name;

        current = -1;
        returns = ConstArray.array();
    }

    protected Object
    pipeline(final int message) {
        for (int i = returns.length(); 0 != i--;) {
            if (message == returns.get(i).message) {
                return returns.get(i).value;
            }
        }
        return null;
    }

    static protected final Class<?> Fulfilled = Eventual.ref(0).getClass();
    static private final Class<?> Rejected = Eventual.reject(null).getClass();

    protected Object
    once(final long window, final int message, final NonIdempotent op) {
        if (window != current) {
            returns = ConstArray.array();
            current = window;
        }
        for (int i = returns.length(); 0 != i--;) {
            if (message > returns.get(i).message) {
                if (i + 1 == returns.length()) { break; }

                /*
                 * Previous requests may have failed at lower levels in the
                 * protocol stack, such as an unknown HTTP method, a failed HTTP
                 * precondition, a too big request entity, or an invocation on a
                 * promise.
                 */
                return null;
            }
            if (message == returns.get(i).message) {
                return returns.get(i).value;
            }
        }
        final String guid = null != name ? name+"-"+window+"-"+message : null;
        log_.got(guid, null, op.method);
        Object r;
        try {
            r = op.apply();
            final Promise<?> pr = Eventual.ref(r);
            if (Fulfilled.isInstance(pr)) {
                r = pr.call();
                log_.fulfilled(guid);
            } else {
                if (Rejected.isInstance(pr)) { pr.call(); }
                log_.resolved(guid);
            }
        } catch (final Exception e) {
            r = Eventual.reject(e);
            log_.rejected(guid, e);
        }
        returns = returns.with(new Return(message, r));
        if (null != r || void.class != op.method.getReturnType()) {
            log_.returned(null != guid ? guid + "-return" : null);
        }
        return r;
    }
}
