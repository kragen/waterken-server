// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;

/**
 * A queued HTTP request.
 */
/* package */ interface
Operation {
    
    /**
     * Render the request.
     * @param sessionKey    message session key
     * @param window        messaging window number
     * @param index         intra-<code>window</code> index
     * @return corresponding request
     * @throws Exception    any problem
     */
    Message<Request>
    render(final String sessionKey, long window, int index) throws Exception;
    
    /**
     * Process the corresponding response.
     * @param request   GUID for request message
     * @param response  received HTTP response
     */
    void fulfill(String request, Message<Response> response);
    
    /**
     * Process the corresponding rejection.
     * @param request   GUID for request message
     * @param reason    reason response will never be provided
     */
    void reject(String request, Exception reason);
}
