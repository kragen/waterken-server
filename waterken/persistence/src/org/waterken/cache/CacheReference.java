// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.cache;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;

/**
 * A {@link Cache} entry.
 */
public class
CacheReference<K,V> extends SoftReference<V> {

    /**
     * cache entry key
     */
    public final K key;

    /**
     * Constructs an instance.
     * @param k {@link #key}
     * @param v {@linkplain #get value}
     * @param q reference queue 
     */
    public
    CacheReference(final K k, final V v, final ReferenceQueue<? super V> q) {
        super(v, q);
        this.key = k;
    }
}
