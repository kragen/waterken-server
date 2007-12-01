// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http.file;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Generates an ETag for a file
 */
public interface
Tag {

    /**
     * Generates an ETAG for a file.
     * @param file  file to tag
     */
    String
    run(File file) throws FileNotFoundException;
}
