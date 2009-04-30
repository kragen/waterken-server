// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import org.joe_e.Powerless;
import org.ref_send.Record;
import org.ref_send.deserializer;

/**
 * Signals a {@link Promise#call call} to an unresolved promise.
 */
public final class
Unresolved extends Exception implements Powerless, Record {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs an instance.
     */
    public @deserializer
    Unresolved() {}
}
