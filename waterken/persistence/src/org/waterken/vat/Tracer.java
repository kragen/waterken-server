// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import org.ref_send.log.Trace;

/**
 * Permission to produce stack traces.
 */
public interface
Tracer {

    /**
     * Gets the current stack trace.
     */
    Trace
    get();
}
