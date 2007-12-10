// Copyright 2003-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;

/**
 * Signals a requested identifier is unavailable for use.
 */
public class
Unavailable extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    Unavailable() {}
}
