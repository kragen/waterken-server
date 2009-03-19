// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.dir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.archive.Archive;
import org.waterken.uri.Path;

/**
 * A directory tree.
 */
public final class
Directory extends Struct implements Archive, Serializable {
    static private final long serialVersionUID = 1L;

    private final FileMetadata meta;
    private final File root;
    
    /**
     * Constructs an instance.
     * @param meta  ETag generator
     * @param root  parent directory of all files in the archive
     */
    public @deserializer
    Directory(@name("meta") final FileMetadata meta,
              @name("root") final File root) {
        if (null == root) { throw new NullPointerException(); }

        this.meta = meta;
        this.root = root;
    }
    
    // java.lang.Iterable interface

    public Iterator<Archive.Entry>
    iterator() {
        final ArrayList<File> todo = new ArrayList<File>();
        todo.add(root);
        return new Iterator<Archive.Entry>() {

            public boolean
            hasNext() { return !todo.isEmpty(); }

            public Archive.Entry
            next() {
                if (todo.isEmpty()) { throw new NoSuchElementException(); }
                final File r = todo.remove(todo.size() - 1);
                if (r.isDirectory()) {
                    try {
                        final ConstArray<File> children = Filesystem.list(r);
                        for (int i = children.length(); 0 != i--;) {
                            final File child = children.get(i);
                            if (child.isFile() || child.isDirectory()) {
                                todo.add(child);
                            }
                        }
                    } catch (final IOException e) {}
                }
                return new Entry(meta, r);
            }

            public void
            remove() { throw new UnsupportedOperationException(); }
        };
    }
    
    static private final class
    Entry extends Struct implements Archive.Entry, Serializable {
        static private final long serialVersionUID = 1L;

        private final FileMetadata meta;
        private final File file;
        
        Entry(final FileMetadata meta, final File file) {
            this.meta = meta;
            this.file = file;
        }

        public String
        getPath() { return file.getPath(); }

        public boolean
        isDirectory() { return file.isDirectory(); }

        public String
        getETag() { return meta.tag(file); }

        public long
        getLength() {
            try {
                return Filesystem.length(file);
            } catch (final FileNotFoundException e) {
                return 0;
            }
        }

        public InputStream
        open() throws IOException { return Filesystem.read(file); }
    }
    
    // org.waterken.archive.Archive interface

    public Archive.Entry
    find(final String path) throws IOException {
        try {
            File r = root;
            for (final String segment : Path.walk(path)) {
                r = Filesystem.file(r, segment);
            }
            return r.isFile() || r.isDirectory() ? new Entry(meta, r) : null;
        } catch (final InvalidFilenameException e) {
            return null;
        }
    }
}
