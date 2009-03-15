// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.dir;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * File meta data.
 */
public interface
FileMetadata {

    /**
     * Generates an ETAG for a file.
     * @param file  file to tag
     */
    String tag(File file) throws FileNotFoundException;
}
