// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joe_e.file.InvalidFilenameException;

/**
 * An update transaction on a {@link Store}.
 */
public interface
Update {
    
    /**
     * Does an entry with the given name already exist?
     * @param filename  name of entry to check for
     * @return <code>true</code> if and only if the named entry exists;
     *         <code>false</code> otherwise
     * @throws Exception                any I/O problem
     */
    boolean
    includes(String filename) throws Exception;
    
    /**
     * Opens an existing file for reading.
     * @param filename  name of file to open
     * @return corresponding input stream
     * @throws InvalidFilenameException <code>filename</code> not valid
     * @throws FileNotFoundException    <code>filename</code> does not exist
     * @throws Exception                any I/O problem
     */
    InputStream
    read(String filename) throws InvalidFilenameException,
                                 FileNotFoundException, Exception;
    
    /**
     * Terminates this transaction.
     * <p>
     * The query interface MUST NOT be accessed after this method is called.
     * </p>
     * @throws Exception                any I/O problem
     */
    void
    close() throws Exception;

    /**
     * Creates an update file.
     * <p>
     * Only one output stream can be active at a time.
     * </p>
     * @param filename  name of file to create
     * @return corresponding output stream
     * @throws InvalidFilenameException <code>filename</code> not valid
     * @throws Exception                any I/O problem
     */
    OutputStream
    write(String filename) throws InvalidFilenameException, Exception;
   
    /**
     * Creates a nested {@link Store}.
     * @param filename  name of nested store
     * @return created {@link Store}
     * @throws InvalidFilenameException <code>filename</code> not available
     * @throws Exception                any I/O problem
     */
    Store
    nest(String filename) throws InvalidFilenameException, Exception;

    /**
     * Commits all changes to the {@link Store}.
     * @throws Exception                any I/O problem
     */
    void
    commit() throws Exception;
}
