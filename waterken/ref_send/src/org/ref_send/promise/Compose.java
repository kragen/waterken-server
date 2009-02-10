// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.joe_e.inert;
import org.joe_e.reflect.Reflection;
import org.ref_send.type.Typedef;

/**
 * Implementation plumbing that users should ignore.
 * @param <P> parameter type
 * @param <R> return type
 */
public final class
Compose<P,R> extends Do<P,Void> implements Serializable {
    static private final long serialVersionUID = 1L;

    private final Do<P,R> block;
    private final Resolver<R> resolver;
    
    /**
     * Constructs a call return block.
     * @param block     code block to execute
     * @param resolver  code block's return resolver
     */
    public
    Compose(final Do<P,R> block, final Resolver<R> resolver) {
        this.block = block;
        this.resolver = resolver;
    }
    
    // org.ref_send.promise.eventual.Do interface
    
    public Void
    fulfill(final P a) {
        final R r;
        try {
            r = block.fulfill(a);
        } catch (final Exception e) {
            resolver.reject(e);
            return null;
        }
        resolver.run(r);
        return null;
    }

    public Void
    reject(final Exception reason) {
        final R r;
        try {
            r = block.reject(reason);
        } catch (final Exception e) {
            resolver.reject(e);
            return null;
        }
        resolver.run(r);
        return null;
    }
    
    // org.ref_send.promise.eventual.Compose interface
    
    /**
     * Determines the corresponding fulfill method.
     * @param p block's parameter type
     * @param x block to introspect on
     */
    static protected Member
    fulfiller(final Class<?> p,
              final @inert Do<?,?> x) throws NoSuchMethodException {
        final @inert Do<?,?> inner =
            x instanceof Compose ? ((Compose<?,?>)x).block : x;
        if (inner instanceof Invoke) {
            final Method m = ((Invoke<?>)inner).method;
            return Modifier.isStatic(m.getModifiers())
                ? m
            : Reflection.method(p, m.getName(), m.getParameterTypes());
        }
        return Reflection.method(inner.getClass(), "fulfill", Object.class);
    }
    
    /**
     * Determines the corresponding reject method.
     * @param x block to introspect on
     */
    static protected Member
    rejecter(final @inert Do<?,?> x) throws NoSuchMethodException {
        final @inert Do<?,?> inner =
            x instanceof Compose ? ((Compose<?,?>)x).block : x;
        return inner instanceof Invoke
            ? ((Invoke<?>)inner).method
        : Reflection.method(inner.getClass(), "reject", Exception.class);
    }

    /**
     * Do block parameter type
     */
    static private final TypeVariable<?> P = Typedef.var(Do.class, "P");
    
    /**
     * Determines a block's parameter type.
     * @param x block to introspect on
     * @return <code>x</code>'s parameter type
     */
    static public Type
    parameter(final @inert Do<?,?> x) {
        final @inert Do<?,?> inner =
            x instanceof Compose ? ((Compose<?,?>)x).block : x;
        return inner instanceof Invoke
            ? ((Invoke<?>)inner).method.getDeclaringClass()
        : Typedef.value(P, inner.getClass());
    }

    /**
     * Do block return type
     */
    static private final TypeVariable<?> R = Typedef.var(Do.class, "R");
    
    /**
     * Determines a block's return type.
     * @param p block's parameter type
     * @param x block to introspect on
     * @return <code>x</code>'s return type
     */
    static protected Type
    output(final Class<?> p, final @inert Do<?,?> x) {
        final @inert Do<?,?> inner =
            x instanceof Compose ? ((Compose<?,?>)x).block : x;
        return inner instanceof Invoke
            ? Typedef.bound(((Invoke<?>)inner).method.getGenericReturnType(), p)
        : Typedef.value(R, inner.getClass());
    }
}