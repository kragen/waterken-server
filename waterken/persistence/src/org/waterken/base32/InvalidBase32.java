// Copyright 2004-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.base32;

import org.joe_e.Powerless;

/**
 * Signals an invalid base32 encoding.
 */
public class
InvalidBase32 extends RuntimeException implements Powerless {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public
    InvalidBase32() {}
}
