// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

import java.io.Serializable;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;

/**
 * A condition on a {@link Variable} {@linkplain Setter#set assignment}.
 * @param <T> value type
 */
public class
Guard<T> extends Struct implements Immutable, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    Guard() {}
    
    // org.ref_send.var.Guard interface
    
    /**
     * Tests a candidate value.
     * @param candidate value to test
     * @return vouched for value
     * @throws RuntimeException <code>candidate</code> does not pass the test
     */
    public T
    run(final T candidate) { throw new RuntimeException(); }
}
