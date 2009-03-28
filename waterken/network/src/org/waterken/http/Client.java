// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.InputStream;

import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.waterken.io.limited.TooBig;
import org.waterken.uri.Header;

/**
 * The response processor for a pending HTTP {@linkplain Server#serve request}.
 */
public abstract class
Client {
    
    /**
     * Is this client still interested in the response?
     * @return <code>true</code> if request should continue,
     *         else <code>false</code>
     */
    public boolean
    isStillWaiting() { return true; }
    
    /**
     * The request cannot be completed.
     * @param reason    reason for failure
     */
    public void
    fail(final Exception reason) throws Exception {
        if (reason instanceof TooBig) {
            receive(Response.tooBig(), null);
        } else {
            receive(new Response("HTTP/1.1", "500",
                                 Reflection.getName(reason.getClass()),
                                 PowerlessArray.array(
                                     new Header("Content-Length", "0")
                                 )), null);
        }
    }

    /**
     * Receive the response to an HTTP request.
     * @param head  response head
     * @param body  response content, or <code>null</code> if none
     */
    public abstract void
    receive(Response head, InputStream body) throws Exception;
}
