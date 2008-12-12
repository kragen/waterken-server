// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.InputStream;

/**
 * The processor for a pending HTTP {@linkplain Server#serve request}.
 */
public interface
Client {

    /**
     * Receive the response to an HTTP request.
     * @param head  response head
     * @param body  response body, or <code>null</code> if none
     */
    void receive(Response head, InputStream body) throws Exception;
    
    /**
     * No response will be provided.
     * @param reason    reason the request failed
     */
    void failed(Exception reason) throws Exception;
}
