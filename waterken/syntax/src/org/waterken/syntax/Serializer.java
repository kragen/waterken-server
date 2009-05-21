// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.lang.reflect.Type;

import org.joe_e.Powerless;
import org.joe_e.inert;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;

/**
 * An object serializer.
 */
public interface
Serializer extends Powerless {
    
    /**
     * Serializes an object.
     * @param export    reference exporter
     * @param type      implict type for <code>value</code>  
     * @param value     value to serialize
     * @return serialized content
     */
    ByteArray serialize(Exporter export, Type type,
                        @inert Object value) throws Exception;
    
    /**
     * Serializes a tuple.
     * @param export    reference exporter
     * @param types     each implict type for <code>values</code>  
     * @param values    values to serialize
     * @return serialized content
     */
    ByteArray serializeTuple(Exporter export, ConstArray<Type> types,
                             @inert ConstArray<?> values) throws Exception;
}
