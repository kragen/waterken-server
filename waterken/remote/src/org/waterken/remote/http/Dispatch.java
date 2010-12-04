// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static java.lang.reflect.Modifier.isStatic;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.reflect.Reflection;

/**
 * Member dispatch for ref_send.
 */
/* package */ final class
Dispatch extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Is the method name overloaded?
     */
    public final boolean overloaded;
    
    /**
     * concrete implementation dispatched to
     */
    public final Method impl;
    
    /**
     * corresponding public declaration for {@link #impl}
     */
    public final Method decl;
    
    private
    Dispatch(final Method impl, final Method decl) {
        overloaded = false;
        this.impl = impl;
        this.decl = decl;
    }
    
    private
    Dispatch() {
        overloaded = true;
        impl = null;
        decl = null;
    }
    
    static private final Dispatch Overloaded = new Dispatch();
    
    /**
     * Finds a named property accessor.
     * @param target    invocation target
     * @param name      property name
     * @return corresponding method, or <code>null</code> if not found
     */
    static public Dispatch
    get(final Object target, final String name) {
        final Class<?> type = null != target ? target.getClass() : Void.class;
        final boolean c = Class.class == type;
        Method impl = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            final int flags = m.getModifiers();
            if (c == isStatic(flags)) {
                if (name.equals(property(m)) &&
                        (!c || target == m.getDeclaringClass())) {
                    if (null != impl) { return null; }
                    impl = m;
                }
            }
        }
        if (null == impl) { return null; }
        final Method decl = bubble(impl);
        if (null == decl) { return null; }
        return new Dispatch(impl, decl);
    }

    /**
     * Finds a named method.
     * @param target    invocation target
     * @param name      method name
     * @return corresponding method, or <code>null</code> if not found
     */
    static public Dispatch
    post(final Object target, final String name) {
        final String methodName= null==name || "".equals(name) ? "apply" : name;
        final Class<?> type = null != target ? target.getClass() : Void.class;
        final boolean c = Class.class == type;
        Method impl = null;
        Method bridge = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            if (c == isStatic(m.getModifiers())) {
                if (methodName.equals(m.getName()) && null == property(m) &&
                        (!c || target == m.getDeclaringClass())) {
                    if (null != impl) {
                        if (null != bridge) { return Overloaded; }
                        if (impl.isBridge()) {
                            bridge = impl;
                            impl = m;
                        } else if (m.isBridge()) {
                            bridge = m;
                        } else { return Overloaded; }
                    } else {
                        impl = m;
                    }
                }
            }
        }
        if (null == impl) { return null; }
        final Method decl = bubble(null != bridge ? bridge : impl);
        if (null == decl) { return null; }
        return new Dispatch(impl, decl);
    }
    
    /**
     * Gets the corresponding property name.
     * <p>
     * This method implements the standard Java beans naming conventions.
     * </p>
     * @param method    candidate method
     * @return name, or null if the method is not a property accessor
     */
    static protected String
    property(final Method method) {
        final String name = method.getName();
        String r =
            name.startsWith("get") &&
            (name.length() == "get".length() ||
             Character.isUpperCase(name.charAt("get".length()))) &&
            method.getParameterTypes().length == 0
                ? name.substring("get".length())
            : (name.startsWith("is") &&
               (name.length() != "is".length() ||
                Character.isUpperCase(name.charAt("is".length()))) &&
               method.getParameterTypes().length == 0
                ? name.substring("is".length())
            : null);
        if (null != r && 0 != r.length() &&
                (1 == r.length() || !Character.isUpperCase(r.charAt(1)))) {
            r = Character.toLowerCase(r.charAt(0)) + r.substring(1);
        }
        return r;
    }

    /**
     * Finds the first invokable declaration of a public method.
     */
    static private Method
    bubble(final Method method) {
        final Class<?> declarer = method.getDeclaringClass();
        if (Object.class == declarer || Struct.class == declarer) {return null;}
        if (Modifier.isPublic(declarer.getModifiers())) { return method; }
        if (Modifier.isStatic(method.getModifiers())) { return null; }
        final String name = method.getName();
        final Class<?>[] param = method.getParameterTypes();
        for (final Class<?> i : declarer.getInterfaces()) {
            try {
                final Method r = bubble(Reflection.method(i, name, param));
                if (null != r) { return r; }
            } catch (final NoSuchMethodException e) {}
        }
        final Class<?> parent = declarer.getSuperclass();
        if (null != parent) {
            try {
                final Method r = bubble(Reflection.method(parent, name, param));
                if (null != r) { return r; }
            } catch (final NoSuchMethodException e) {}
        }
        return null;
    }
}
