// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;

/**
 * Signals an attempt to {@linkplain Menu#grow add} too many
 * {@linkplain Menu#getEntries entries} to a {@link Menu}.
 */
public class
TooMany extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    TooMany() {}
}
