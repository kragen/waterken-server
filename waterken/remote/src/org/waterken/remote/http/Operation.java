// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.ref_send.promise.eventual.Do;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;

/**
 * A queued HTTP request.
 */
/* package */ abstract class
Operation extends Do<Message<Response>,Void> implements Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Render the request. 
     * @return corresponding request
     * @throws Exception    any problem
     */
    protected abstract Message<Request>
    render() throws Exception;
}
