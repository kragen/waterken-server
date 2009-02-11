// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.custom;

import org.ref_send.promise.Volatile;

/**
 * A query interface to a [ name =&gt; value ] store.
 */
public interface
Query<T> extends Interpreted {

    /**
     * Gets an identified value.
     * @param name  value identifier
     * @return corresponding value, or <code>null</code> if none
     */
    <R extends T> Volatile<R> get(String name);
}
