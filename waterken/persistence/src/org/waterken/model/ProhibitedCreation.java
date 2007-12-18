// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals an attempt to create selfish state in an {@link Model#extend}
 * {@link Model#enter transaction}.
 */
public class
ProhibitedCreation extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;
    
    /**
     * created object type
     */
    public final Class type;
    
    /**
     * Constructs an instance.
     * @param type  {@link #type}
     */
    public @deserializer
    ProhibitedCreation(@name("type") final Class type) {
        super(type.getName());
        this.type = type;
    }
}
