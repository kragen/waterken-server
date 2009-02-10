// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Struct;

/**
 * Ignore all notifications.
 * @param <T> value type
 */
public final class
Sink<T> extends Struct implements Receiver<T>, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Does nothing.
     */
    public void
    run(final T value) {}
}
