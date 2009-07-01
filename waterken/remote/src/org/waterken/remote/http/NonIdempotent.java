// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import org.waterken.http.Message;
import org.waterken.http.Response;

/**
 * A non-idempotent request handler.
 */
interface NonIdempotent {
    Message<Response>
    apply(String message) throws Exception;
}
