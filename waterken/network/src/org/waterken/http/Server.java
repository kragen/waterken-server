// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import org.ref_send.promise.eventual.Do;

/**
 * An HTTP server.
 */
public interface
Server {

    /**
     * cache-control max-age value for an immutable resource
     */
    int forever = 365 * 24 * 60 * 60;
    
    /**
     * cache-control max-age value for a mutable resource
     */
    int ephemeral = 0;

    /**
     * Eventually process a request.
     * @param resource  absolute resource identifier
     * @param request   request
     * @param respond   corresponding response processor
     * @throws Exception    any problem which should terminate the connection
     */
    void
    serve(String resource, Request request,
                           Do<Response,?> respond) throws Exception;
}
