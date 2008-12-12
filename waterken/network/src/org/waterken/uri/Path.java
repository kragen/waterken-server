// Copyright 2002-2004 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

import java.io.File;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.joe_e.charset.URLEncoding;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;

/**
 * URI path manipulation.
 */
public final class
Path {

    private
    Path() {}

    /**
     * Canonicalizes a path.
     * @param path  candidate path
     * @return canonicalized path
     * @throws InvalidURI   rejected <code>path</code>
     */
    static public String
    vet(String path) throws InvalidURI {

        // Check for disallowed characters.
        for (int i = path.length(); i-- != 0;) {
            final char c = path.charAt(i);
            if (!(URI.pchar(c) || '/' == c)) { throw new InvalidURI(); }
        }

        // Remove any "./" segments.
        while (path.startsWith("./")) {
            path = path.substring("./".length());
        }
        while (true) {
            final int start_rel = path.indexOf("/./");
            if (start_rel == -1) { break; }
            path = path.substring(0, start_rel) +
                   path.substring(start_rel + "/.".length());
        }
        if (path.endsWith("/.")) {
            path = path.substring(0, path.length() - ".".length());
        } else if (path.equals(".")) {
            path = "";
        }

        // Unwind any ".." segments.
        while (true) {
            while (path.startsWith("../")) {
                path = path.substring("../".length());
            }
            if (path.equals("..")) { return ""; }
            final int start_rel = path.indexOf("/../");
            if (start_rel == -1) { break; }
            path = path.substring(0, path.lastIndexOf('/', start_rel - 1) + 1) +
                   path.substring(start_rel + "/../".length());
        }

        // Unwind a trailing "..".
        if (path.endsWith("/..")) {
            path = path.substring(0,
                path.lastIndexOf('/', path.length() - "x/..".length()) + 1);
        }

        // Make sure it's not an absolute path.
        if (path.startsWith("/")) { throw new InvalidURI(); }

        return path;
    }

    /**
     * Gets the resource folder.
     * @param path  canonicalized path
     * @return <code>path</code>, less the last segment
     */
    static public String
    folder(String path) { return path.substring(0, path.lastIndexOf('/') + 1); }

    /**
     * Extracts the resource name.
     * @param path  canonicalized path
     * @return unescaped path segment
     */
    static public String
    name(final String path) {
        return URLEncoding.decode(path.substring(path.lastIndexOf('/') + 1));
    }

    /**
     * Walk the segments in a path.
     * @param path  canonicalized path
     * @return unescaped path segment sequence
     */
    static public Iterable<String>
    walk(final String path) {
        return new Iterable<String>() {

            public Iterator<String>
            iterator() {
                return new Iterator<String> () {

                    private int i = 0 == path.length() ? -1 : 0;
                    private int j = path.indexOf('/');

                    public boolean
                    hasNext() { return -1 != i; }

                    public String
                    next() {
                        if (-1 == i) { throw new NoSuchElementException(); }
                        final String segment;
                        if (-1 == j) {
                            segment = path.substring(i);
                            i = -1;
                        } else {
                            segment = path.substring(i, j);
                            i = j + 1;
                            j = path.indexOf('/', i);
                            if (-1 == j && i == path.length()) {
                                i = -1;
                            }
                        }
                        return URLEncoding.decode(segment);
                    }

                    public void
                    remove() { throw new UnsupportedOperationException(); }
                };
            }
        };
    }
    
    /**
     * Walks down a file path.
     * @param root  root folder
     * @param path  canonicalized path to walk
     * @return named file
     * @throws InvalidFilenameException invalid name in <code>path</code> 
     */
    static public File
    descend(final File root, final String path) throws InvalidFilenameException{
        File r = root;
        for (final String segment : walk(path)) {
            if (segment.startsWith(".")) {throw new InvalidFilenameException();}
            r = Filesystem.file(r, segment);
        }
        return r;
    }
}
