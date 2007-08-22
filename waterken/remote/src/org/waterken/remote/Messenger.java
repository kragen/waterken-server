// Copyright 2005-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.lang.reflect.Method;

import org.ref_send.promise.eventual.Do;

/**
 * The remote messaging interface.
 */
public interface
Messenger {
    
    /**
     * Settles a remote promise.
     * @param <P> observer's parameter type
     * @param <R> observer's return type
     * @param URL       reference identifier
     * @param R         observer's return type
     * @param observer  promise observer
     */
    <P,R> R
    when(String URL, Class<?> R, Do<P,R> observer);
    
    /**
     * Invokes a remote object.
     * @param URL       reference identifier
     * @param proxy     target object
     * @param method    method to invoke
     * @param arg       each argument
     * @return return reference
     */
    Object
    invoke(String URL, Object proxy, Method method, Object... arg);
}
