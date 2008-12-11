// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.inert;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Bi-directional [ URI &lt;=&gt; resource ] URI namespace lookup.
 */
public class
Namespace extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * find a named resource
     */
    public final Importer connect;
    
    /**
     * find the corresponding name for a resource
     */
    public final Exporter export;
    
    /**
     * Constructs an instance.
     * @param connect   {@link #connect}
     * @param export    {@link #export}
     */
    public @deserializer
    Namespace(@name("connect") @inert final Importer connect,
              @name("export") @inert final Exporter export) {
        this.connect = connect;
        this.export = export;
    }
}
