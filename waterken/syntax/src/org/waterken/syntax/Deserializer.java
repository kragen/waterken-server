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
     * Deserializes an argument list.
     * @param base          base URL
     * @param connect       reference importer
     * @param code          class loader
     * @param content       serialized content
     * @param parameters    each expected type
     * @return each deserialized argument
     * @throws Exception    any exception
     */
    ConstArray<?>
    run(String base, Importer connect, ClassLoader code,
        InputStream content, ConstArray<Type> parameters) throws Exception;
}
