// Copyright 2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/**
 * @author Tyler Close
 */
package org.joe_e;

/**
 * This is the interface for custom implementations to handle 
 * <code>java.lang.Error</code>s in a Joe-E program.  The error handler should
 * abort the computation of the current thread so as to prevent recovery from
 * the error.
 */
public interface ErrorHandler extends Powerless {

    /**
     * Handles an error.
     * @param err error to handle
     * @return error to throw
     */
    Error handle(Error err);
}
