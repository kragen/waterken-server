// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.OutputStream;

import org.joe_e.Powerless;
import org.joe_e.array.ConstArray;

/**
 * An object serializer.
 */
public interface
Serializer extends Powerless {
    
    /**
     * Serializes an argument list.
     * @param export    reference exporter
     * @param values    each argument to serialize
     * @param out       serialized content output, will be flushed and closed
     */
    void
    run(Exporter export, ConstArray<?> values,
        OutputStream out) throws Exception;
}
