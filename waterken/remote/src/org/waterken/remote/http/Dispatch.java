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
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Method dispatch implementation for web_send.
 */
/* package */ class
Dispatch extends Struct implements Record, Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * concrete implementation dispatched to
     */
    public final Method implementation;
    
    /**
     * corresponding public declaration for {@link #implementation}
     */
    public final Method declaration;
    
    /**
     * Constructs an instance.
     * @param implementation    {@link #implementation}
     * @param declaration       {@link #declaration}
     */
    public @deserializer
    Dispatch(@name("implementation") final Method implementation,
             @name("declaration") final Method declaration) {
        this.implementation = implementation;
        this.declaration = declaration;
    }
    
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
        Method implementation = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            final int flags = m.getModifiers();
            if (c == isStatic(flags)) {
                if (name.equals(property(m))) {
                    if (null != implementation) { return null; }
                    implementation = m;
                }
            }
        }
        if (null == implementation) { return null; }
        final Method declaration = bubble(implementation);
        if (null == declaration) { return null; }
        return new Dispatch(implementation, declaration);
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
        Method implementation = null;
        Method bridge = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            final int flags = m.getModifiers();
            if (c == isStatic(flags)) {
                if (methodName.equals(m.getName()) && null == property(m)) {
                    if (null != implementation) {
                        if (null != bridge) { return null; }
                        if (implementation.isBridge()) {
                            bridge = implementation;
                            implementation = m;
                        } else if (m.isBridge()) {
                            bridge = m;
                        } else {
                            return null;
                        }
                    } else {
                        implementation = m;
                    }
                }
            }
        }
        if (null == implementation) { return null; }
        final Method declaration=bubble(null!=bridge ? bridge : implementation);
        if (null == declaration) { return null; }
        return new Dispatch(implementation, declaration);
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
