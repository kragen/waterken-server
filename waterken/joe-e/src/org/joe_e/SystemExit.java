// Copyright 2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/**
 * @author Tyler Close
 */
package org.joe_e;

import java.io.Serializable;

/**
 * Executes a {@link System#exit} in response to an Error being encountered.
 * The exit status indicates the the type of error.
 */
final class SystemExit extends Struct implements ErrorHandler, Serializable {
    static private final long serialVersionUID = 1L;

    public Error handle(final Error error) {
        int status = 0x80000000;
        
        if (error instanceof VirtualMachineError) {
            status |= 0x00010000;
            if (error instanceof InternalError) {
                status |= 0x00000100;
            } else if (error instanceof OutOfMemoryError) {
                status |= 0x00000200;
            } else if (error instanceof StackOverflowError) {
                status |= 0x00000300;
            } else if (error instanceof UnknownError) {
                status |= 0x00000400;
            }
        } else if (error instanceof AssertionError) {
            status |= 0x00020000;
        } else if (error instanceof ThreadDeath) {
            status |= 0x00030000;
        }

        while (true) {
            try {
                System.exit(status);
                break;
            } catch (final Throwable e) {
                // Keep trying...
            }
        }
        return error;   // This statement should never execute, but having it
                        // here makes the compiler happier.
    }
}
