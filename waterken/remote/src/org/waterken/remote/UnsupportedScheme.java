// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals an attempt to message using a URI of an unsupported scheme.
 */
public class
UnsupportedScheme extends Exception implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * unsupported scheme name
     */
    public final String scheme;
    
    /**
     * Constructs an instance.
     * @param scheme    {@link #scheme}
     */
    public @deserializer
    UnsupportedScheme(@name("scheme") final String scheme) {
        this.scheme = scheme;
    }
}
