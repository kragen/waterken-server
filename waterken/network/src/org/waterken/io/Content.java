// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io;

import java.io.OutputStream;

/**
 * An object that can serialize itself to a binary stream.
 */
public interface
Content {
    
    /**
     * preferred chunk size for writing to the output stream
     */
    int chunkSize = 1280;

    /**
     * Writes the serialization to an output stream.
     * @param out   output stream
     * @throws Exception  any exception
     */
    void
    writeTo(OutputStream out) throws Exception;
}
