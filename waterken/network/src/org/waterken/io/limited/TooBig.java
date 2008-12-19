// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.limited;

import java.io.EOFException;

import org.joe_e.Powerless;

/**
 * Signals a size limit was reached.
 */
public class
TooBig extends EOFException implements Powerless {
    static private final long serialVersionUID = 1L;
}
