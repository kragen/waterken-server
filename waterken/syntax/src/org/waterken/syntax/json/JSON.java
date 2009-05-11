// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import java.lang.reflect.Modifier;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ArrayBuilder;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.scope.Layout;

/**
 * Java &lt;=&gt; JSON naming conventions.
 */
public final class
JSON {
    private JSON() {}

    /**
     * encoding of a rejected promise
     */
    static public final Layout Rejected = new Layout(PowerlessArray.array("!"));
    
    /**
     * Enumerate all types implemented by a class.
     */
    static public PowerlessArray<String>
    types(final Class<?> actual) {
        final Class<?> end =
            Struct.class.isAssignableFrom(actual) ? Struct.class : Object.class;
        final PowerlessArray.Builder<String> r = PowerlessArray.builder(4);
        for (Class<?> i=actual; end!=i; i=i.getSuperclass()) { ifaces(i, r); }
        return r.snapshot();
    }

    /**
     * List all the interfaces implemented by a class.
     */
    static private void
    ifaces(final Class<?> type, final ArrayBuilder<String> r) {
        if (Modifier.isPublic(type.getModifiers())) {
            try {
                if (0 != Reflection.methods(type).length()) {
                    r.append(name(type));
                }
            } catch (final Exception e) {}
        }
        for (final Class<?> i : type.getInterfaces()) { ifaces(i, r); }
    }
    
    static private final class
    Alias extends Struct implements Powerless {
        final Class<?> type;
        final String name;
        
        Alias(final Class<?> type, final String name) {
            this.type = type;
            this.name = name;
        }
    }
    
    /**
     * custom typenames
     */
    static private final PowerlessArray<Alias> custom = PowerlessArray.array(
        new Alias(Object.class, "object"),
        new Alias(String.class, "string"),
        new Alias(Number.class, "number"),
        new Alias(RuntimeException.class, "Error"),
        new Alias(Exception.class, "Error"),
        new Alias(java.lang.reflect.Method.class, "function"),
        new Alias(Class.class, "class"),
        new Alias(ClassCastException.class, "Mismatch"),
        new Alias(NullPointerException.class, "NaO"),
        new Alias(ArithmeticException.class, "NaN"),
        new Alias(org.joe_e.array.ConstArray.class, "array")
    );
    
    static protected String
    name(final Class<?> type) throws IllegalArgumentException {
        for (final Alias a : custom) {
            if (type == a.type) { return a.name; }
        }
        return Reflection.getName(type).replace('$', '-');
    }
    
    static protected Class<?>
    load(final ClassLoader code,
         final String name) throws ClassNotFoundException {
        for (final Alias a : custom) {
            if (a.name.equals(name)) { return a.type; }
        }
        return "boolean".equals(name)
            ? boolean.class
        : "byte".equals(name)
            ? byte.class
        : "char".equals(name)
            ? char.class
        : "double".equals(name)
            ? double.class
        : "float".equals(name)
            ? float.class
        : "int".equals(name)
            ? int.class
        : "long".equals(name)
            ? long.class
        : "short".equals(name)
            ? short.class
        : "void".equals(name)
            ? void.class
        : code.loadClass(name.replace('-', '$'));
    }
}
