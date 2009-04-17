// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joe_e.file.InvalidFilenameException;

/**
 * An update transaction on a {@link Store}.
 */
public interface
Update {
    
    /**
     * Terminates this transaction.
     * <p>
     * Further invocations MUST NOT be made on this object.
     * </p>
     */
    void close();
    
    /**
     * Opens an existing file for reading.
     * @param filename  name of file to open
     * @return corresponding input stream
     * @throws FileNotFoundException    <code>filename</code> does not exist
     * @throws IOException              any I/O problem
     */
    InputStream read(String filename) throws FileNotFoundException, IOException;

    /**
     * Creates an update file.
     * <p>
     * Only one output stream can be active at a time.
     * </p>
     * @param filename  name of file to create
     * @return corresponding output stream
     * @throws InvalidFilenameException <code>filename</code> not valid
     * @throws IOException              any I/O problem
     */
    OutputStream write(String filename) throws InvalidFilenameException,
                                               IOException;
   
    /**
     * Creates a nested {@link Store}.
     * @param filename  name of nested store
     * @return created {@link Store}
     * @throws InvalidFilenameException <code>filename</code> not available
     * @throws IOException              any I/O problem
     */
    Store nest(String filename) throws InvalidFilenameException, IOException;

    /**
     * Commits all changes to the {@link Store}.
     * @throws IOException              any I/O problem
     */
    void commit() throws IOException;
}
