// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals an attempt to modify persistent state in a {@link Database#query}
 * {@link Database#enter transaction}.
 */
public class
ProhibitedModification extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;
    
    /**
     * modified class typename
     */
    public final String typename;
    
    /**
     * Constructs an instance.
     * @param typename  {@link #typename}
     */
    public @deserializer
    ProhibitedModification(@name("typename") final String typename) {
        super(typename);
        this.typename = typename;
    }
}
