// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals an attempt to message an object identified by an unknown URL scheme.
 */
public class
UnknownScheme extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * URL scheme
     */
    public final String scheme;
    
    /**
     * Constructs an instance.
     * @param scheme    {@link #scheme}
     */
    public @deserializer
    UnknownScheme(@name("scheme") final String scheme) {
        this.scheme = scheme;
    }
}
