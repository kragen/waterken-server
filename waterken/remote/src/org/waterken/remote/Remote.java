// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.joe_e.Token;
import org.joe_e.reflect.Proxies;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Deferred;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.vat.Root;
import org.waterken.vat.Vat;

/**
 * A remote reference.
 */
public final class
Remote extends Deferred<Object> implements Promise<Object> {
    static private final long serialVersionUID = 1L;

    /**
     * local address space
     */
    private final Root local;

    /**
     * reference relative URL
     */
    private final String URL;

    /**
     * Constructs an instance.
     * @param local local address space
     * @param URL   reference relative URL
     */
    public
    Remote(final Root local, final String URL) {
        super((Eventual)local.fetch(null, Vat._),
              (Token)local.fetch(null, Vat.deferred));
        if (null == URL) { throw new NullPointerException(); }
        this.local = local;
        this.URL = URL;
    }
    
    // java.lang.Object interface

    /**
     * Is the given object the same?
     * @param x The compared to object.
     * @return true if the same, else false.
     */
    public boolean
    equals(final Object x) {
        return x instanceof Remote && _.equals(((Remote)x)._) &&
               URL.equals(((Remote)x).URL) &&
               local.equals(((Remote)x).local);
    }
    
    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0x4E307E4F; }

    // org.ref_send.promise.Volatile interface

    /**
     * @return <code>this</code>
     */
    public Object
    cast() { return this; }
    
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
            final Messenger messenger = local.fetch(null, Vat.messenger);
            return messenger.invoke(URL, proxy, method, arg);
        } catch (final Exception e) { throw new Error(e); }
    }
    
    // org.ref_send.promise.eventual.Deferred interface

    protected <R> R
    when(final Class<?> R, final Do<Object,R> observer) {
        final Messenger messenger = local.fetch(null, Vat.messenger);
        return messenger.when(URL, R, observer);
    }
    
    // org.waterken.remote.Remote interface
    
    /**
     * Accesses the wrapped URL.
     * @param root  local address space
     * @return the wrapped URL
     */
    public String
    export(final Root root) {
        if (!local.equals(root)) { throw new ClassCastException(); }
        return URL;
    }

    /**
     * Is the given object pass-by-construction?
     * @param object  candidate object
     * @return <code>true</code> if pass-by-construction,
     *         else <code>false</code>
     */
    static public boolean
    isPBC(final Object object) {
        final Class<?> type = null != object ? object.getClass() : Void.class;
        return String.class == type ||
            Integer.class == type ||
            Long.class == type ||
            Boolean.class == type ||
            Byte.class == type ||
            Short.class == type ||
            Character.class == type ||
            Double.class == type ||
            Float.class == type ||
            Void.class == type ||
            java.math.BigInteger.class == type ||
            java.math.BigDecimal.class == type ||
            org.ref_send.Record.class.isAssignableFrom(type) ||
            Throwable.class.isAssignableFrom(type) ||
            org.joe_e.array.ConstArray.class.isAssignableFrom(type) ||
            org.ref_send.promise.Volatile.class.isAssignableFrom(type);
    }
}
