// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import org.joe_e.Equatable;

/**
 * A log interface.
 */
public interface
Log {

    /**
     * Logs a conditional message send.
     * @param message   message identifier
     * @param condition condition identifier
     */
    void
    sentIf(Equatable message, Equatable condition);
    
    /**
     * Logs resolution of a condition.
     * @param condition condition identifier
     */
    void
    resolved(Equatable condition);
    
    /**
     * Logs receipt of a message.
     * @param message   message identifier
     */
    void
    got(Equatable message);
}
