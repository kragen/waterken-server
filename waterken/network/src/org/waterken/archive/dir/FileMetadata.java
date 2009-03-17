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
     * Generates an ETag for a file.
     * @param file  file to tag
     * @return corresponding ETag, or <code>null</code> if file does not exist
     */
    String tag(File file);
}
