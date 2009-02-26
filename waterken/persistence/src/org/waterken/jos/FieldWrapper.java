// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.InvalidClassException;
import java.io.Serializable;
import java.lang.reflect.Field;

import org.joe_e.Powerless;
import org.joe_e.Struct;

/**
 * A persistent field.
 */
/* package */ final class
FieldWrapper extends Struct implements Powerless, Serializable {
    static private final long serialVersionUID = 1;

    private final Class<?> declarer;
    private final String name;
    
    FieldWrapper(final Field code) {
        declarer = code.getDeclaringClass();
        name = "" + code.getName(); // no interning for idempotent serialization
    }
    
    // org.waterken.jos.Wrapper interface
    
    private Object
    readResolve() throws InvalidClassException {
        try {
            return declarer.getField(name);
        } catch (final NoSuchFieldException e) {
            throw new InvalidClassException(declarer.getName() + "#" + name);
        }
    }
}
