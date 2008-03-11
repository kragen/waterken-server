// Copyright 2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.file;

import org.joe_e.Powerless;

/**
 * Signals an invalid filename.
 */
public class InvalidFilenameException extends RuntimeException 
                                        implements Powerless {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public InvalidFilenameException() {}
}
