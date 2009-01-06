// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.joe_e.Equatable;
import org.joe_e.Immutable;
import org.joe_e.JoeE;
import org.joe_e.Selfless;
import org.joe_e.Token;
import org.joe_e.array.ConstArray;
import org.joe_e.reflect.Proxies;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Volatile;
import org.ref_send.type.Typedef;

/**
 * Implementation hook that users should ignore.
 * @param <T> referent type
 */
public abstract class
Deferred<T> implements Volatile<T>, InvocationHandler, Selfless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * weak promise type
     */
    static public final Class<?> WeakPromise = WeakPromise.class;
    
    /**
     * corresponding eventual operator
     */
    protected final Eventual _;

    /**
     * Constructs an instance.
     * @param _         corresponding eventual operator
     * @param deferred  {@link Deferred} permission
     */
    protected
    Deferred(final Eventual _, final Token deferred) {
        // *MUST ONLY* allow construction by a caller who directly possesses the
        // corresponding deferred permission. The Eventual implementation relies
        // upon this check being done in this constructor.
        if (_.deferred != deferred) { throw new ClassCastException(); }
        this._ = _;
    }

    // java.lang.Object interface

    public abstract boolean
    equals(Object x);

    public abstract int
    hashCode();

    // java.lang.reflect.InvocationHandler interface

    /**
     * Forwards a Java language invocation.
     * @param proxy     eventual reference
     * @param method    method to invoke
     * @param arg       each invocation argument
     * @return eventual reference for the invocation return
     */
    public Object
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
            return when(Typedef.raw(Typedef.bound(method.getGenericReturnType(),
                                                  proxy.getClass())),
                new Invoke<T>(method,null==arg ? null : ConstArray.array(arg)));
        } catch (final Exception e) { throw new Error(e); }
    }

    // org.ref_send.promise.Volatile interface

    public abstract T
    cast() throws Exception;

    // org.ref_send.promise.eventual.Deferred interface
    
    /**
     * Notifies an observer in a future event loop turn.
     * @param <R> observer's return type
     * @param R         observer's return type
     * @param observer  promise observer
     * @return promise, or {@linkplain Eventual#cast eventual reference}, for
     *         the <code>observer</code>'s return, or <code>null</code> if the
     *         <code>observer</code>'s return type is <code>Void</code>
     */
    protected abstract <R> R
    when(Class<?> R, Do<T,R> observer);
    
    /**
     * Creates a remote reference.
     * @param type  referent type
     */
    public Object
    _(final Class<?> type) { return _.cast(type, this);  }
    
    /**
     * Creates a remote reference that mimics the interface of a concrete type.
     * @param concrete  type to mimic
     */
    public @SuppressWarnings("unchecked") T
    mimic(final Class<?> concrete) {
        // build the list of types to implement
        Class<?>[] types = virtualize(concrete);
        boolean selfless = false;
        for (final Class<?> i : types) {
            selfless = Selfless.class.isAssignableFrom(i);
            if (selfless) { break; }
        }
        if (!selfless) {
            final int n = types.length;
            System.arraycopy(types, 0, types = new Class[n + 1], 0, n);
            types[n] = Selfless.class;
        }
        return (T)Proxies.proxy(this, types);
    }

    /**
     * Lists the allowed interfaces implemented by a type.
     * @param base  base type
     * @return allowed interfaces implemented by <code>base</code>
     */
    static private Class<?>[]
    virtualize(final Class<?> base) {
        Class<?>[] r = base.getInterfaces();
        int i = r.length;
        final Class<?> parent = base.getSuperclass();
        if (null != parent && Object.class != parent) {
            final Class<?>[] p = virtualize(parent);
            if (0 != p.length) {
                System.arraycopy(r, 0, r = new Class<?>[i + p.length], 0, i);
                System.arraycopy(p, 0, r, i, p.length);
            }
        }
        while (i-- != 0) {
            final Class<?> type = r[i];
            if (type == Serializable.class ||
                    !Proxies.isImplementable(type) ||
                    JoeE.isSubtypeOf(type, Immutable.class) ||
                    JoeE.isSubtypeOf(type, Equatable.class)) {
                final Class<?>[] x = virtualize(r[i]);
                final Class<?>[] c = r;
                r = new Class<?>[c.length - 1 + x.length];
                System.arraycopy(c, 0, r, 0, i);
                System.arraycopy(x, 0, r, i, x.length);
                System.arraycopy(c, i + 1, r, i + x.length, c.length - (i+1));
            }
        }
        return r;
    }
}
