// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import org.joe_e.Powerless;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals a failed attempt to deserialize a cyclic Java object graph.
 */
public class
CyclicGraph extends RuntimeException implements Powerless, Record {
    static private final long serialVersionUID = 1L;

    /**
     * each typename in the cycle
     */
    public final PowerlessArray<String> cycle;
    
    /**
     * Constructs an instance.
     * @param cycle {@link #cycle}
     */
    public @deserializer
    CyclicGraph(@name("cycle") final PowerlessArray<String> cycle) {
        this.cycle = cycle;
    }
}
