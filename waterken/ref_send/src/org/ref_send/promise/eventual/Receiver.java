// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

/**
 * An output receiver.
 * @param <T> value type
 */
public interface
Receiver<T> {

    /**
     * Receives a value.
     * @param value received value
     */
    void
    run(T value);
}
