// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import org.joe_e.file.InvalidFilenameException;

/**
 * Signals a {@link Update#nest naming} collision.
 */
public class
NameCollision extends InvalidFilenameException {
    static private final long serialVersionUID = 1L;
}
