// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Member;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Task;

/**
 * The server-side state associated with a messaging session.
 */
/* package */ final class
ServerSideSession implements Serializable {
    static private final long serialVersionUID = 1L;

    private final String name;          // GUID of this messaging session
    private final Log log;              // corresponding log output
    
    private long current;               // current window number
    private ConstArray<Object> returns; // return values in the current window
    
    protected
    ServerSideSession(final String name, final Log log) {
        this.name = name;
        this.log = log;
        
        current = -1;
        returns = ConstArray.array();
    }
    
    protected Object
    get(final long window, final int message) {
        if (window != current) { throw new IndexOutOfBoundsException(); }
        return returns.get(message);
    }
    
    protected Object
    once(final long window, final int message,
         final Member member, final Task<?> op){
        if (window == current) {
            if (message != returns.length()) { return returns.get(message); }
        } else {
            current = window;
            returns = ConstArray.array();
        }
        log.got(name + "-" + window + "-" + message, member);
        Object r;
        try {
            r = op.run();
        } catch (final Exception e) {
            r = new Rejected<Object>(e);
        }
        returns = returns.with(r);
        log.sent(name + "-" + window + "-" + message + "-return");
        return r;
    }
    
}
