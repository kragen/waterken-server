// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive;

import java.io.IOException;
import java.io.InputStream;

/**
 * Read-only access to a collection of files.
 */
public interface
Archive extends Iterable<Archive.Entry> {
    
    /**
     * An {@link Archive} entry.
     */
    static public interface
    Entry {
        
        /**
         * Gets the file path.
         */
        String getPath();
        
        /**
         * Is this entry a directory?
         * @return <code>true</code> if a directory, else <code>false</code>
         */
        boolean isDirectory();
        
        /**
         * Gets the version identifier.
         */
        String getETag();
        
        /**
         * Gets the length of corresponding {@linkplain data stream #open}.
         */
        long getLength();
        
        /**
         * Opens the corresponding data stream.
         */
        InputStream open() throws IOException;
    }
    
    /**
     * Gets the named file.
     * @param path  path to file
     * @return corresponding entry, or <code>null</code> if file does not exist
     * @throws IOException              any I/O problem
     */
    Entry find(String path) throws IOException;
}
