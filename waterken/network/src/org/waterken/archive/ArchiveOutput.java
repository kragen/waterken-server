// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive;

import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link Archive} creator.
 */
public interface
ArchiveOutput {
    
    /**
     * Appends an entry to the archive.
     * @param name  name of entry to append
     * @return output stream for corresponding data
     */
    OutputStream append(String name);
    
    /**
     * Finish the archive.
     * @throws IOException  any I/O problem
     */
    void finish() throws IOException ;
}
