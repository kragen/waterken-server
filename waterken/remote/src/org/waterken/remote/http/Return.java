// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.joe_e.reflect.Proxies;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Deferred;
import org.ref_send.promise.eventual.Do;

/**
 * 
 */
/* package */ final class
Return extends Deferred<Object> implements Promise<Object> {
    static private final long serialVersionUID = 1L;

    private final Caller session;
    private final long window;
    private final int index;
    private Promise<Object> underlying;
    
    // java.lang.Object interface
    
    public boolean
    equals(final Object x) {
        // TODO Auto-generated method stub
        return false;
    }
    
    public int
    hashCode() { return 0x4E7E424F; }
    
    // org.ref_send.promise.Volatile interface
    
    public Object
    cast() throws Exception { return underlying.cast(); }
    
    // java.lang.reflect.InvocationHandler interface

    @Override public Object
    invoke(final Object proxy, final Method method,
           final Object[] arg) throws Exception {
        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(method.getName())) {
                return arg[0] instanceof Proxy &&
                    proxy.getClass() == arg[0].getClass() &&
                    equals(Proxies.getHandler((Proxy)arg[0]));
            } else {
                return Reflection.invoke(method, this, arg);
            }
        }
        try {
            return messenger.invoke(href, proxy, method, arg);
        } catch (final Exception e) { throw new Error(e); }
    }
    
    // org.ref_send.promise.eventual.Deferred interface
    
    protected <R> R
    when(final Class<?> R, final Do<Object,R> observer) {
        // TODO Auto-generated method stub
        return null;
    }
}
