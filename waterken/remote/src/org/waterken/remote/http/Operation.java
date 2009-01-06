// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import org.waterken.http.Message;
import org.waterken.http.Request;

/**
 * A queued HTTP request.
 */
/* package */ interface
Operation {
    
    /**
     * Render the request. 
     * @return corresponding request
     * @throws Exception    any problem
     */
    Message<Request> render() throws Exception;
}
