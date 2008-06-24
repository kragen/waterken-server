// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.lang.reflect.Proxy;

/**
 * Replace classes and objects as they are read in.
 */
class
SubstitutionStream extends ObjectInputStream {

    private final ClassLoader code;
    
    SubstitutionStream(final boolean resolve, final ClassLoader code,
                       final InputStream in) throws IOException {
        super(in);
        this.code = code;
        if (resolve) { enableResolveObject(true); }
    }

    protected Class<?>
    resolveClass(final ObjectStreamClass d) throws IOException,
                                                   ClassNotFoundException {
        final String name = d.getName();
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
        : Class.forName(name, false, code);
    }
    
    protected Class<?>
    resolveProxyClass(final String[] typenames) throws IOException,
                                                       ClassNotFoundException {
        final Class<?>[] interfaces = new Class[typenames.length];
        for (int i = typenames.length; 0 != i--;) {
            interfaces[i] = Class.forName(typenames[i], false, code);
        }
        try {
            return Proxy.getProxyClass(code, interfaces);
        } catch (final IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }
}
