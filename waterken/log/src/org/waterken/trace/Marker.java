// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace;

import org.ref_send.log.Anchor;

/**
 * An event anchor generator.
 */
public interface
Marker {
    
    /**
     * Generates an event anchor.
     */
    Anchor apply();
}
