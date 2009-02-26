// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ConstArray;

/**
 * A persistent method.
 */
/* package */ final class
MethodWrapper extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1;

    private final Class<?> declarer;
    private final String name;
    private final ConstArray<Class<?>> params;
    
    MethodWrapper(final Method code) {
        declarer = code.getDeclaringClass();
        name = "" + code.getName(); // no interning for idempotent serialization
        params = ConstArray.array(code.getParameterTypes());
    }
    
    private Object
    readResolve() throws InvalidClassException {
        try {
            return declarer.getMethod(name, params.toArray(new Class<?>[0]));
        } catch (final NoSuchMethodException e) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(declarer.getName());
            buffer.append("#");
            buffer.append(name);
            buffer.append("(");
            for (int i = 0; i != params.length(); ++i) {
                if (0 != i) { buffer.append(","); }
                buffer.append(params.get(i).getName());
            }
            buffer.append(")");
            throw new InvalidClassException(buffer.toString());
        }
    }
}
