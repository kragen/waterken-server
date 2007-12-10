// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

/**
 * A condition on a {@link Variable} {@linkplain Setter#set assignment}.
 * @param <T> value type
 */
public interface
Guard<T> {

    /**
     * Tests a candidate value.
     * @param candidate value to test
     * @return vouched for value
     * @throws RuntimeException <code>candidate</code> does not pass the test
     */
    T
    run(T candidate);
}
