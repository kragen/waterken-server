// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * MIME data.
 */
public final class
MIME extends Struct implements Powerless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * known MIME types
     */
    public final PowerlessArray<FileType> known;
    
    /**
     * Constructs an instance.
     * @param known {@link #known}
     */
    public @deserializer
    MIME(@name("known") final PowerlessArray<FileType> known) {
        this.known = known;
    }
}
