// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals an attempt to modify persistent state in an {@link Vat#extend}
 * {@link Vat#enter transaction}.
 */
public class
ProhibitedModification extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;
    
    /**
     * modified object type
     */
    public final Class type;
    
    /**
     * Constructs an instance.
     * @param type  {@link #type}
     */
    public @deserializer
    ProhibitedModification(@name("type") final Class type) {
        super(type.getName());
        this.type = type;
    }
}
