// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive;

import java.io.IOException;
import java.io.InputStream;

/**
 * A read-only [ name =&gt; byte stream ] mapping.
 */
public interface
Archive extends Iterable<Archive.Entry> {
    
    /**
     * An {@link Archive} entry.
     */
    static public interface
    Entry {
        
        /**
         * Gets the entry name.
         */
        String getName();
        
        /**
         * Gets the version identifier.
         */
        String getETag();
        
        /**
         * Gets the length of the corresponding {@linkplain data stream #open}.
         */
        long getLength();
        
        /**
         * Opens the corresponding data stream.
         */
        InputStream open() throws IOException;
    }
    
    /**
     * Finds a named entry.
     * @param name  name of entry to find
     * @return corresponding entry, or <code>null</code> if one does not exist
     * @throws IOException              any I/O problem
     */
    Entry find(String name) throws IOException;
}
