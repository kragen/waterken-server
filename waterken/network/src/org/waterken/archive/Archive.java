// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A set of files.
 */
public interface
Archive {
    
    /**
     * Gets the length of an existing file.
     * @param filename  name of file to measure
     * @return number of bytes, or <code>-1</code> if file does not exist
     * @throws IOException              any I/O problem
     */
    long measure(String filename) throws IOException;
    
    /**
     * Gets the version tag for an existing file.
     * @param filename  name of file to version
     * @return corresponding ETag, or <code>null</code> if file does not exist
     * @throws IOException              any I/O problem
     */
    String tag(String filename) throws IOException;
    
    /**
     * Opens an existing file for reading.
     * @param filename  name of file to open
     * @return corresponding input stream
     * @throws FileNotFoundException    <code>filename</code> does not exist
     * @throws IOException              any I/O problem
     */
    InputStream read(String filename) throws FileNotFoundException, IOException;
}
