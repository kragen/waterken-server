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
        final ArrayList<Entry> todo = new ArrayList<Entry>();
        list(todo, new Entry(meta, "", root));
        return new Iterator<Archive.Entry>() {

            public boolean
            hasNext() { return !todo.isEmpty(); }

            public Archive.Entry
            next() {
                if (todo.isEmpty()) { throw new NoSuchElementException(); }
                final Entry r = todo.remove(todo.size() - 1);
                if (r.path.endsWith("/")) { list(todo, r); }
                return r;
            }

            public void
            remove() { throw new UnsupportedOperationException(); }
        };
    }
    
    static private void
    list(final ArrayList<Entry> todo, final Entry dir) {
        final ConstArray<File> files;
        try {
            files = Filesystem.list(dir.file);
        } catch (final IOException e) { return; }
        for (int i = files.length(); 0 != i--;) {
            final File f = files.get(i);
            if (f.isFile()) {
                todo.add(new Entry(dir.meta, dir.path + f.getName(), f));
            } else if (f.isDirectory()) {
                todo.add(new Entry(dir.meta, dir.path + f.getName() + '/', f));
            }
        }
    }
    
    static private final class
    Entry extends Struct implements Archive.Entry, Serializable {
        static private final long serialVersionUID = 1L;

        protected final FileMetadata meta;
        protected final String path;
        protected final File file;
        
        Entry(final FileMetadata meta, final String path, final File file) {
            this.meta = meta;
            this.path = path;
            this.file = file;
        }

        public String
        getName() { return path; }

        public String
        getETag() {
            final long version = meta.getLastModified(file);
            return version > 0 ? '\"' + Long.toHexString(version) + '\"' : null;
        }

        public long
        getLength() {
            try { return Filesystem.length(file);
            } catch (final FileNotFoundException e) { return 0; }
        }

        public InputStream
        open() throws IOException { return Filesystem.read(file); }
    }
    
    // org.waterken.archive.Archive interface

    public Archive.Entry
    find(final String path) throws IOException {
        try {
            File f = root;
            for (final String segment : Path.walk(path)) {
                f = Filesystem.file(f, segment);
            }
            return path.endsWith("/")
                ? (f.isDirectory() ? new Entry(meta, path, f) : null)
            : (f.isFile() ? new Entry(meta, path, f) : null);
        } catch (final InvalidFilenameException e) { return null; }
    }
}
