// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Constructor;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ConstArray;

/**
 * A persistent constructor. 
 */
/* package */ final class
ConstructorWrapper extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1;

    private final Class<?> declarer;
    private final ConstArray<Class<?>> params;
    
    ConstructorWrapper(final Constructor<?> code) {
        declarer = code.getDeclaringClass();
        params = ConstArray.array(code.getParameterTypes());
    }

    // org.waterken.jos.Wrapper interface
    
    private Object
    readResolve() throws InvalidClassException { 
        try {
            return declarer.getConstructor(params.toArray(new Class<?>[0]));
        } catch (final NoSuchMethodException e) {
            final StringBuilder buffer = new StringBuilder();
            buffer.append(declarer.getName());
            buffer.append("#(");
            for (int i = 0; i != params.length(); ++i) {
                if (0 != i) { buffer.append(","); }
                buffer.append(params.get(i).getName());
            }
            buffer.append(")");
            throw new InvalidClassException(buffer.toString());
        }
    }
}
