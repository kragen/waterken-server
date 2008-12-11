// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace;

import java.lang.reflect.Member;

import org.ref_send.log.Trace;

/**
 * Permission to produce stack traces.
 */
public interface
Tracer {
    
    /**
     * Gets the text message from an exception.
     * @param e exception to extract message from
     */
    String
    readException(Throwable e);
    
    /**
     * Gets the stack trace for a given exception.
     * @param e exception to trace
     */
    Trace
    traceException(Throwable e);

    /**
     * Gets the current stack trace.
     */
    Trace
    traceHere();
    
    /**
     * Produces a dummy stack trace for a method.
     * @param lambda    sole member of the dummy stack trace
     */
    Trace
    dummy(Member lambda);
}
