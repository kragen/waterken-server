// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.limited;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;

import java.io.IOException;

/**
 * Signals that too much data has been requested from a stream.
 */
public class
TooMuchData extends IOException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    TooMuchData() {}
}
