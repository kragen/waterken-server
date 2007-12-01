// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html

package org.waterken.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.waterken.http.file.Tag;

/**
 * Generates a tag based on the file's last modified time.
 */
final class
LastModified extends Struct implements Tag, Immutable, Serializable {
    static private final long serialVersionUID = 1L;

    // org.waterken.http.file.Tag interface

    public String
    run(final File file) throws FileNotFoundException {
        if (!file.isFile()) { throw new FileNotFoundException(); }
        return '\"' + Long.toHexString(file.lastModified()) + '\"';
    }
}
