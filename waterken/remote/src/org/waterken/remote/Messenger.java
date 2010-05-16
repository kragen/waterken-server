// Copyright 2005-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.lang.reflect.Method;

import org.ref_send.promise.Do;

/**
 * The inter-vat messaging interface.
 */
public interface
Messenger {
    
    /**
     * Settles a remote promise.
     * @param href      relative URL string for message target
     * @param proxy     local proxy object
     * @param T         concrete referent type, <code>null</code> if not known
     * @param observer  promise observer
     */
    void when(String href, Remote proxy, Class<?> T, Do<Object,?> observer);
    
    /**
     * Invokes a remote object.
     * @param href      relative URL string for message target
     * @param proxy     local proxy object
     * @param method    method to invoke
     * @param arg       each argument
     * @return return reference
     */
    Object invoke(String href, Object proxy, Method method, Object... arg);
}
