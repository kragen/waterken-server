// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.lang.reflect.Method;

/**
 * A non-idempotent request handler.
 */
/* package */ abstract class
NonIdempotent {

    /**
     * traced call target
     */
    protected final Method method;

    protected
    NonIdempotent(final Method method) {
        this.method = method;
    }

    /**
     * Processes the request.
     * @return  {@link method} return value
     * @throws Exception    any exception
     */
    protected abstract
    Object apply() throws Exception;
}
