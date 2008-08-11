// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.type;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Type definition manipulation.
 */
public final class
Typedef {

    private
    Typedef() {}

    /**
     * Gets the named type variable.
     * @param declaration   generic declaration
     * @param name          variable name
     * @return corresponding type variable
     * @throws NullPointerException no matching variable found
     */
    static public <T> TypeVariable<Class<T>>
    var(final Class<T> declaration, final String name) {
        for (final TypeVariable<Class<T>> i : declaration.getTypeParameters()) {
            if (i.getName().equals(name)) { return i; }
        }
        throw new NullPointerException();
    }

    /**
     * Gets the value of a type variable.
     * @param p type variable
     * @param a instantiated type
     * @return type argument, or <code>null</code> if <code>p</code> is not from
     *         <code>a</code> or one of its super types
     */
    static public Type
    value(final TypeVariable<?> p, final Type a) {
        Type r;
        final Class<?> template = raw(a);
        final GenericDeclaration declaration = p.getGenericDeclaration();
        if (declaration.equals(template)) {
            r = p;
        } else {
            r = null;
            for (final Type i : template.getGenericInterfaces()) {
                r = value(p, i);
                if (null != r) { break; }
            }
            if (null == r) {
                final Type extended = template.getGenericSuperclass();
                if (null != extended) {
                    r = value(p, extended);
                }
            }
        }
        if (r instanceof TypeVariable) {
            if (a instanceof ParameterizedType) {
                int i = 0;
                for (final TypeVariable<?> v : declaration.getTypeParameters()){
                    if (p.equals(v)) { break; }
                    ++i;
                }
                r = ((ParameterizedType)a).getActualTypeArguments()[i];
            } else {
                r = Object.class;   // Class implements the raw type.
            }
        }
        return r;
    }

    /**
     * Determine the corresponding raw type.
     * @param type  generic type
     * @return corresponding raw type
     */
    static public Class<?>
    raw(final Type type) {
        return type instanceof Class
            ? (Class<?>)type
        : (type instanceof ParameterizedType
            ? raw(((ParameterizedType)type).getRawType())
        : (type instanceof WildcardType
            ? raw(((WildcardType)type).getUpperBounds()[0])
        : Object.class));
    }
    
    /**
     * Determine the upper bound on a type parameter.
     * @param parameter generic parameter type
     * @param declared  generic class type
     * @return upper bound on the actual type
     */
    static public Type
    bound(final Type parameter, final Type declared) {
        final Type argument = parameter instanceof TypeVariable
            ? value((TypeVariable<?>)parameter, declared)
        : parameter;
        return argument instanceof WildcardType
            ? ((WildcardType)argument).getUpperBounds()[0]
        : argument;
    }
 }
