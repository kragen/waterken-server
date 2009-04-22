// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.InputStream;
import java.lang.reflect.Type;

import org.joe_e.Powerless;

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
     * @return each deserialized argument
     * @throws Exception    any exception
     */
    Object run(String base, Importer connect, Type type,
               ClassLoader code, InputStream content) throws Exception;
}
