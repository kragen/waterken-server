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

/**
 * A remote reference.
 */
public final class
Remote extends Deferred<Object> implements Promise<Object> {
    static private final long serialVersionUID = 1L;

    /**
     * network message sender
     */
    private final Messenger messenger;

    /**
     * relative URL for message target
     */
    private final String URL;

    /**
     * Constructs an instance.
     * @param _         corresponding eventual operator
     * @param deferred  {@link Deferred} permission
     * @param messenger network message sender
     * @param URL       relative URL for message target
     */
    public
    Remote(final Eventual _, final Token deferred,
           final Messenger messenger, final String URL) {
        super(_, deferred);
        if (null == URL) { throw new NullPointerException(); }
        this.messenger = messenger;
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
        return x instanceof Remote &&
               URL.equals(((Remote)x).URL) &&
               messenger.equals(((Remote)x).messenger) &&
               _.equals(((Remote)x)._);
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
            return messenger.invoke(URL, proxy, method, arg);
        } catch (final Exception e) { throw new Error(e); }
    }
    
    // org.ref_send.promise.eventual.Deferred interface

    protected <R> R
    when(final Class<?> R, final Do<Object,R> observer) {
        return messenger.when(URL, R, observer);
    }
    
    // org.waterken.remote.Remote interface
    
    /**
     * Accesses the message target URL.
     * @param messenger network message sender
     * @return relative URL for message target
     */
    public String
    export(final Messenger messenger) {
        if (!this.messenger.equals(messenger)) {throw new ClassCastException();}
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
            Boolean.class == type ||
            Long.class == type ||
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
