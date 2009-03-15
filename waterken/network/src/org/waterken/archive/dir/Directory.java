// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.dir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.archive.Archive;

/**
 * Read access to all files in a directory.
 */
public final class
Directory extends Struct implements Archive, Serializable {
    static private final long serialVersionUID = 1L;

    private final File parent;
    private final FileMetadata meta;
    
    /**
     * Constructs an instance.
     * @param parent    parent directory of all files in the archive
     * @param meta      ETag generator
     */
    public @deserializer
    Directory(@name("parent") final File parent,
              @name("meta") final FileMetadata meta) {
        if (null == parent) { throw new NullPointerException(); }

        this.parent = parent;
        this.meta = meta;
    }
    
    // org.waterken.archive.Archive interface
    
    public long
    measure(final String filename) throws IOException {
        try {
            return Filesystem.length(Filesystem.file(parent, filename));
        } catch (final InvalidFilenameException e) { return -1;
        } catch (final FileNotFoundException e)    { return -1; }
    }

    public String
    tag(final String filename) throws IOException {
        try {
            return meta.tag(Filesystem.file(parent, filename));
        } catch (final InvalidFilenameException e) { return null;
        } catch (final FileNotFoundException e)    { return null; }
    }

    public InputStream
    read(final String filename) throws FileNotFoundException, IOException {
        return Filesystem.read(Filesystem.file(parent, filename));
    }
}
