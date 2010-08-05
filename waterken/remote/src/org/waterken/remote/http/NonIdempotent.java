// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

/**
 * A non-idempotent request handler.
 */
interface NonIdempotent {
    Object apply(String message);
}
