// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace;

import org.ref_send.log.Anchor;
import org.ref_send.promise.Task;

/**
 * An event anchor generator.
 */
public interface
Marker extends Task<Anchor> {
    
    /**
     * Generates an event anchor.
     */
    Anchor run();
}
