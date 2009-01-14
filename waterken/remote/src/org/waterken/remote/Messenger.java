// Copyright 2005-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.lang.reflect.Method;

import org.ref_send.promise.eventual.Do;

/**
 * The inter-vat messaging interface.
 */
public interface
Messenger {
    
    /**
     * Settles a remote promise.
     * @param <R> observer's return type
     * @param href      target URL string
     * @param proxy     local proxy object
     * @param R         observer's return type
     * @param observer  promise observer
     */
    <R> R
    when(String href, Remote proxy, Class<?> R, Do<Object,R> observer);
    
    /**
     * Invokes a remote object.
     * @param href      target URL string
     * @param proxy     local proxy object
     * @param method    method to invoke
     * @param arg       each argument
     * @return return reference
     */
    Object
    invoke(String href, Object proxy, Method method, Object... arg);
}
