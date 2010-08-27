// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import org.joe_e.Powerless;

/**
 * Signals a non-final public instance field in a pass-by-copy type.
 */
public class
NonFinalRecordField extends NullPointerException implements Powerless {
    static private final long serialVersionUID = 1L;
}
