// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.dir;

import java.io.File;

/**
 * File meta data.
 */
public interface
FileMetadata {
    
    /**
     * Gets the last modified time of a file.
     * @param file  file to check
     * @return corresponding last modified time, or -1 if file does not exist
     */
    long getLastModified(File file);
}
