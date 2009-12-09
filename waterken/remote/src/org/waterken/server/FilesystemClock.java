// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.Serializable;

import org.joe_e.Struct;
import org.waterken.archive.dir.FileMetadata;

/* package */ final class
FilesystemClock extends Struct implements FileMetadata, Serializable {
    static private final long serialVersionUID = 1L;

    // org.waterken.archive.dir.FileMetadata interface

    public long
    getLastModified(final File file) {
        return file.isFile() ? file.lastModified() : -1;
    }
}
