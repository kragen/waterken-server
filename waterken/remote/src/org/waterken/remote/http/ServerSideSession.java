// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.waterken.http.Message;
import org.waterken.http.Response;
import org.waterken.uri.Header;

/**
 * The server-side state associated with a messaging session.
 */
/* package */ final class
ServerSideSession implements Serializable {
    static private final long serialVersionUID = 1L;

    private final String name;                  // GUID of this session
    
    private long current;                           // current window number
    private ConstArray<Message<Response>> returns;  // returns in current window
    
    protected
    ServerSideSession(final String name) {
        this.name = name;
        
        current = -1;
        returns = ConstArray.array();
    }
    
    static protected Message<Response>
    execute(final String message, final NonIdempotent op) {
        try {
            return op.apply(message);
        } catch (final Exception e) {
            return new Message<Response>(new Response(
                "HTTP/1.1", "409", Reflection.getName(e.getClass()),
                PowerlessArray.array(new Header("Content-Length", "0"))), null);
        }
    }
    
    protected Message<Response>
    once(final long window, final int message, final NonIdempotent op) {
        if (window != current) {
            returns = ConstArray.array();
            current = window;
        }
        if (message != returns.length()) {
            if (message < returns.length()) { return returns.get(message); }
            for (int i = returns.length(); i != message; i += 1) {
                returns = returns.with(new Message<Response>(new Response(
                    "HTTP/1.1", "404", "never", PowerlessArray.array(
                    		new Header("Content-Length", "0"))), null));
            }
        }
        final Message<Response> r = execute(name+"-"+window+"-"+message, op);
        returns = returns.with(r);
        return r;
    }
}
