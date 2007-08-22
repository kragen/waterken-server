// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.waterken.model.Heap;

/**
 * A persistent method.
 */
final class
MethodWrapper implements Wrapper {
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
        final Class[] parameter = code.getParameterTypes();
        out.writeInt(parameter.length);
        for (int i = 0; i != parameter.length; ++i) {
            out.writeObject(parameter[i]);
        }
    }

    private void
    readObject(final ObjectInputStream in) throws IOException,
                                                  ClassNotFoundException {
        in.defaultReadObject();

        final Class<?> declarer = (Class)in.readObject();
        final String name = in.readUTF();
        final Class[] parameter = new Class[in.readInt()];
        for (int i = 0; i != parameter.length; ++i) {
            parameter[i] = (Class)in.readObject();
        }
        try {
            code = declarer.getMethod(name, parameter);
        } catch (final NoSuchMethodException e) {
            final StringBuffer buffer = new StringBuffer();
            buffer.append(declarer.getName());
            buffer.append("#");
            buffer.append(name);
            buffer.append("(");
            for (int j = 0; j != parameter.length; ++j) {
                if (0 != j) {
                    buffer.append(",");
                }
                buffer.append(parameter[j].getName());
            }
            buffer.append(")");
            throw new ClassNotFoundException(buffer.toString());
        }
    }    

    // org.waterken.jos.Wrapper interface
    
    public Object
    peel(final Heap loader) { return code; }
}
