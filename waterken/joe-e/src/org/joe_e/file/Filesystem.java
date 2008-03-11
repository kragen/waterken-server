// Copyright 2006-2008 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;

import org.joe_e.array.ConstArray;

/**
 * {@link File} construction.  This provides a capability-safe API for access to
 * files.  A File object grants read and write access to a file or directory.
 * Due to limitations in Java, all file references are to textual file names, not
 * file descriptors.  Multiple operations on a File may thus apply to different
 * incarnations of the file.
 */
public final class Filesystem {
    
    private Filesystem() {}
    
    /*
     * The safety of this API relies on all File constructors being tamed away. 
     */
    
    /**
     * Produce a File capability for a file contained in a folder.  The returned
     * object is just a handle; the underlying file may not yet exist.
     * @param folder    containing folder
     * @param child     a single filename component, not a relative path
     * @return a capability for the requested file
     * @throws IllegalArgumentException if <code>folder</code> is null or
     *     the empty path
     */
    static public File file(final File folder, final String child) 
                                       throws InvalidFilenameException {
        // With Java's File(File, String) constructor, either of these can give
        // access to all files on system; null is ambiently available.
        if (folder == null || folder.getPath().equals("")) {
            throw new IllegalArgumentException();
        }
        checkName(child);
        return new File(folder, child);
    }

    /**
     * Vets a filename.  Checks that the argument would be interpreted as a
     * file name rather than as a path or relative directory specifier
     * @param name a single filename component, not a relative path
     * @throws InvalidFilenameException <code>name</code> is rejected\
     */
    static public void checkName(final String name) 
                                       throws InvalidFilenameException {
        // Check for path operation names.
        if (name.equals("") || name.equals(".") || name.equals("..")) {
            throw new InvalidFilenameException();
        }

        // Check for path separator char.
        if (name.indexOf(File.separatorChar) != -1) {
            throw new InvalidFilenameException();
        }      
        // '/' works as a separator on Windows, even though it isn't the 
        // platform separator character
        if ('/' != File.separatorChar && name.indexOf('/') != -1) {
            throw new InvalidFilenameException();
        }
    }
    
    /**
     * List the contents of a directory.
     * @param dir   directory to list
     * @return directory entries, sorted alphabetically
     * @throws IOException <code>dir</code> is not a directory, or an I/O error
     */
    static public ConstArray<File> list(final File dir) throws IOException {
        final File[] contents = dir.listFiles();
        if (contents == null) { 
            throw new IOException();
        }
        Arrays.sort(contents, new Comparator<File>() {
            public int compare(final File a, final File b) {
                return a.getName().compareTo(b.getName());
            }
        });
        return ConstArray.array(contents);
    }
    
    /**
     * Gets the length of a file
     * @param file  file to stat
     * @return the length of the file in bytes
     * @throws FileNotFoundException   <code>file</code> not found
     */
    static public long length(final File file) 
                                        throws FileNotFoundException {
        if (!file.isFile()) { 
            throw new FileNotFoundException();
        }
        return file.length();
    }
    
    /**
     * Opens an existing file for reading.
     * @param file  file to open
     * @return opened input stream
     * @throws FileNotFoundException  <code>file</code> not found
     */
    static public InputStream read(final File file) 
                                       throws FileNotFoundException {
        if (!file.isFile()) { 
            throw new FileNotFoundException();
        }
        return new FileInputStream(file);
    }
    
    /**
     * Creates a file for writing.
     * @param file  file to create
     * @return opened output stream
     * @throws IOException    <code>file</code> could not be created
     */
    static public OutputStream writeNew(final File file) throws IOException {
        if (!file.createNewFile()) {
            throw new IOException();
        }
        return new FileOutputStream(file);
    }
}
