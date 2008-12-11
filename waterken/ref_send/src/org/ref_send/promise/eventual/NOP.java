// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import java.io.Serializable;

import org.joe_e.Equatable;
import org.joe_e.Powerless;

/**
 * A log that discards all events.
 */
public final class
NOP implements Log, Powerless, Equatable, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public
    NOP() {}
    
    // org.ref_send.promise.eventual.Log interface
    
    /**
     * NOP
     */
    public void
    comment(final String text) {}
    
    /**
     * NOP
     */
    public void
    problem(final Exception reason) {}

    /**
     * NOP
     */
    public void
    got(final String message) {}

    /**
     * NOP
     */
    public void
    sent(final String message) {}

    /**
     * NOP
     */
    public void
    sentIf(final String message, final String condition) {}

    /**
     * NOP
     */
    public void
    resolved(final String condition) {}
}
