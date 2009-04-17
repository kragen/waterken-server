// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import java.io.IOException;

import org.joe_e.Powerless;

/**
 * Signals a {@link Store} does not exist.
 */
public class
DoesNotExist extends IOException implements Powerless {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public
    DoesNotExist() {}
}
