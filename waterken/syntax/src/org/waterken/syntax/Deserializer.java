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
     * @param content   serialized content, will be closed
     * @param connect   reference importer
     * @param base      base URL
     * @param code      class loader
     * @param type      expected type of the referenced object
     * @return deserialized object
     * @throws Exception    any exception
     */
    Object deserialize(InputStream content, Importer connect, String base,
                       ClassLoader code, Type type) throws Exception;

    /**
     * Deserializes an argument list.
     * @param content       serialized content, will be closed
     * @param connect       reference importer
     * @param base          base URL
     * @param code          class loader
     * @param parameters    expected type of each argument
     * @return deserialized tuple
     * @throws Exception    any exception
     */
    ConstArray<?> deserializeTuple(InputStream content, Importer connect,
                                   String base, ClassLoader code,
                                   Type... parameters) throws Exception;
}
