// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.joe_e.Powerless;
import org.waterken.db.Root;

/**
 * A persistent method.
 */
final class
MethodWrapper implements Wrapper, Powerless {
    static private final long serialVersionUID = 1;

    private transient Method code;
    
    MethodWrapper(final Method code) {
        this.code = code;
    }
    
    // java.io.Serializable interface

    private void
    writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeObject(code.getDeclaringClass());
        out.writeUTF(code.getName());
        final Class<?>[] params = code.getParameterTypes();
        out.writeInt(params.length);
        for (final Class<?> param : params) { out.writeObject(param); }
    }

    private void
    readObject(final ObjectInputStream in) throws IOException,
                                                  ClassNotFoundException {
        in.defaultReadObject();

        final Class<?> declarer = (Class<?>)in.readObject();
        final String name = in.readUTF();
        final Class<?>[] params = new Class<?>[in.readInt()];
        for (int i = 0; i != params.length; ++i) {
            params[i] = (Class<?>)in.readObject();
        }
        try {
            code = declarer.getMethod(name, params);
        } catch (final NoSuchMethodException e) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(declarer.getName());
            buffer.append("#");
            buffer.append(name);
            buffer.append("(");
            for (int j = 0; j != params.length; ++j) {
                if (0 != j) { buffer.append(","); }
                buffer.append(params[j].getName());
            }
            buffer.append(")");
            throw new ClassNotFoundException(buffer.toString());
        }
    }    

    // org.waterken.jos.Wrapper interface
    
    public Method
    peel(final Root root) { return code; }
}
