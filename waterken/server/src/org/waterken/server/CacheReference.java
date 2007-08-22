// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * A {@link Cache} entry reference.
 */
final class
CacheReference<E> extends SoftReference<E> {

    /**
     * The entry key.
     */
    final String key;

    /**
     * Constructs an instance.
     */
    CacheReference(final E referent,
                   final ReferenceQueue<E> q,
                   final String key) {
        super(referent, q);
        this.key = key;
    }
}
