// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.InputStream;
import java.lang.reflect.Type;

import org.joe_e.Powerless;
import org.joe_e.array.ConstArray;

/**
 * An object deserializer.
 */
public interface
Deserializer extends Powerless {

    /**
     * Deserializes an object.
     * @param base      base URL
     * @param connect   reference importer
     * @param type      expected type
     * @param code      class loader
     * @param content   serialized content input, will be closed
     * @return deserialized object
     * @throws Exception    any exception
     */
    Object deserialize(String base, Importer connect, Type type,
                       ClassLoader code, InputStream content) throws Exception;

    /**
     * Deserializes a tuple.
     * @param base      base URL
     * @param connect   reference importer
     * @param types     each expected type
     * @param code      class loader
     * @param content   serialized content input, will be closed
     * @return deserialized tuple
     * @throws Exception    any exception
     */
    ConstArray<?> deserializeTuple(String base, Importer connect,
                                   ConstArray<Type> types, ClassLoader code,
                                   InputStream content) throws Exception;
}
