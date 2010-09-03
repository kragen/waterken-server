// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.limited;

import java.io.EOFException;

import org.joe_e.Powerless;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals a size limit was reached.
 */
public class
TooBig extends EOFException implements Powerless {
    static private final long serialVersionUID = 1L;
    
    /**
     * actual size
     */
    public final long size;
    
    /**
     * maximum allowed size
     */
    public final long max;
    
    /**
     * Constructs an instance.
     * @param size  {@link #size}
     * @param max   {@link #max}
     */
    public @deserializer
    TooBig(@name("size") final long size,
           @name("max") final long max) {
        super(size + " > " + max);
        this.size = size;
        this.max = max;
    }
}
