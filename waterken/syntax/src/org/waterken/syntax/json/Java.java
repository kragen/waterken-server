// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.json;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;

/**
 * Java &lt;=&gt; JSON naming conventions.
 */
final class
Java {
    private Java() {}
    
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
        new Alias(java.lang.reflect.Method.class, "function"),
        new Alias(Class.class, "class"),
        new Alias(ClassCastException.class, "NoMatch"),
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
