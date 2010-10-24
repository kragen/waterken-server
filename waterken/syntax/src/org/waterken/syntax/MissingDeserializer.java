// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import org.joe_e.Powerless;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals a {@link Record} that does not have a constructor meeting all of the
 * requirements for a {@link deserializer}.
 */
public class
MissingDeserializer extends ClassCastException implements Powerless {
    static private final long serialVersionUID = 1L;
    
    /**
     * name of type that is missing a {@link deserializer}
     */
    public final String typename;
    
    /**
     * Constructs an instance.
     * @param typename  {@link #typename}
     */
    public @deserializer
    MissingDeserializer(@name("typename") final String typename) {
        super(typename);
        this.typename = typename;
    }
}
