// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import java.io.Serializable;

import org.joe_e.Equatable;
import org.joe_e.Powerless;
import org.joe_e.Struct;

/**
 * A log that discards all events.
 */
public final class
Sink extends Struct implements Log, Powerless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public
    Sink() {}
    
    // org.ref_send.promise.eventual.Log interface
    
    /**
     * @return <code>false</code>
     */
    public boolean
    isOn() { return false; }
    
    /**
     * NOP
     */
    public void
    comment(final String text) {}

    /**
     * NOP
     */
    public void
    got(final Equatable message) {}

    /**
     * NOP
     */
    public void
    resolved(final Equatable condition) {}

    /**
     * NOP
     */
    public void
    sentIf(final Equatable message, final Equatable condition) {}
}
