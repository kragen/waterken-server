// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.dns.Resource;

/**
 * Signals a longer {@linkplain Resource#ttl TTL} value is required.
 */
public class
UseLongerTTL extends UnsupportedResource {
    static private final long serialVersionUID = 1L;

    /**
     * minimum acceptable {@linkplain Resource#ttl TTL}
     */
    public final int minTTL;
    
    /**
     * Constructs an instance.
     * @param minTTL    {@link #minTTL}
     */
    public @deserializer
    UseLongerTTL(@name("minTTL") final int minTTL) {
        this.minTTL = minTTL;
    }
}
