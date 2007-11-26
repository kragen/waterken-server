// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * A {@link Cache} entry reference.
 */
final class
CacheReference<K,V> extends SoftReference<V> {

    /**
     * cache entry key
     */
    final K key;

    CacheReference(final V referent,
                   final ReferenceQueue<V> q,
                   final K key) {
        super(referent, q);
        this.key = key;
    }
}
