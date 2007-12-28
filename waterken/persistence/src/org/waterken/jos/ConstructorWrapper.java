// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;

import org.joe_e.Powerless;
import org.waterken.model.Root;

/**
 * A persistent constructor. 
 */
final class
ConstructorWrapper implements Wrapper, Powerless {
    static private final long serialVersionUID = 1;

    private transient Constructor code;
    
    ConstructorWrapper(final Constructor code) {
        this.code = code;
    }
    
    // java.io.Serializable interface
    
    private void
    writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        out.writeObject(code.getDeclaringClass());
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
        final Class[] parameter = new Class[in.readInt()];
        for (int i = 0; i != parameter.length; ++i) {
            parameter[i] = (Class)in.readObject();
        }
        try {
            code = declarer.getConstructor(parameter);
        } catch (final NoSuchMethodException e) {
            final StringBuffer buffer = new StringBuffer();
            buffer.append(declarer.getName());
            buffer.append("#new(");
            for (int i = 0; i != parameter.length; ++i) {
                if (0 != i) {
                    buffer.append(",");
                }
                buffer.append(parameter[i].getName());
            }
            buffer.append(")");
            throw new ClassNotFoundException(buffer.toString());
        }
    }

    // org.waterken.jos.Wrapper interface
    
    public Constructor
    peel(final Root root) { return code; }
}
