// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.InputStream;

/**
 * The response processor for a pending HTTP {@linkplain Server#serve request}.
 */
public interface
Client {

    /**
     * Receive the response to an HTTP request.
     * @param head  response head
     * @param body  response body, or <code>null</code> if none
     */
    void run(Response head, InputStream body) throws Exception;
}
