// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals an attempt to persist non-{@link Selfless} state in a
 * {@link Database#query} {@link Database#enter transaction}.
 */
public class
ProhibitedCreation extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;
    
    /**
     * created object typename
     */
    public final String typename;
    
    /**
     * Constructs an instance.
     * @param typename  {@link #typename}
     */
    public @deserializer
    ProhibitedCreation(@name("typename") final String typename) {
        super(typename);
        this.typename = typename;
    }
}
