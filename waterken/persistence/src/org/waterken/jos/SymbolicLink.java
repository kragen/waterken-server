// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.Serializable;

import org.joe_e.Struct;

/**
 * A {@link Root} binding.
 */
final class
SymbolicLink extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * link target
     */
    final Object target;
    
    /**
     * Constructs an instance.
     * @param target    {@link target}
     */
    SymbolicLink(final Object target) {
        this.target = target;
    }
}
