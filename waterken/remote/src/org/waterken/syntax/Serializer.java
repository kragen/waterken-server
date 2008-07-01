// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import org.joe_e.Powerless;
import org.joe_e.array.ConstArray;
import org.waterken.id.Exporter;
import org.waterken.io.Content;

/**
 * An object serializer.
 */
public interface
Serializer extends Powerless {

    /**
     * indicates the given object should be serialized
     */
    boolean render = false;
    
    /**
     * indicates the given object should be described
     */
    boolean describe = true;
    
    /**
     * Serializes an argument list.
     * @param mode      either {@link #render} or {@link #describe}   
     * @param export    reference exporter
     * @param values    each argument to serialize
     * @return serialized content
     */
    Content
    run(boolean mode, Exporter export, ConstArray<?> values);
}
