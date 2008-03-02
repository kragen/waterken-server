// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.log;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A stack trace.
 */
public class
Trace extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * list of call sites
     */
    public final PowerlessArray<CallSite> calls;

    /**
     * Constructs an instance.
     * @param calls {@link #calls}
     */
    public @deserializer
    Trace(@name("calls") final PowerlessArray<CallSite> calls) {
        this.calls = calls;
    }
}
