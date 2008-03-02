// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

/**
 * An output writer.
 * @param <T> value type
 */
public interface
Receiver<T> {

    /**
     * Receives another output value.
     * @param value received output
     */
    void
    run(T value);
}
