// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.cache;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;

/**
 * A memory sensitive cache.
 */
public final class
Cache<K,V> implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * cache exit queue
     */
    private transient ReferenceQueue<V> wiped;

    /**
     * object cache: [ id =&gt; soft value ]
     */
    private transient HashMap<K,CacheReference<K,V>> entries;

    /**
     * Constructs an instance.
     * @param wiped permission to monitor the garbage collector
     */
    public
    Cache(final ReferenceQueue<V> wiped) {
        if (null == wiped) { throw new NullPointerException(); }
        
        // if the caller has a reference queue, assume it is trusted
        // infrastructure code that is also allowed to compute a hash code for
        // any object
        this.wiped = wiped;
        entries = new HashMap<K,CacheReference<K,V>>();
    }
    
    // java.io.Serializable interface

    /**
     * Restores the transient data structures.
     */
    private void
    readObject(final ObjectInputStream in) throws IOException,
                                                  ClassNotFoundException {
        // Read in the attributes.
        in.defaultReadObject();

        // Setup the cache structures.
        entries = new HashMap<K,CacheReference<K,V>>();
        wiped = new ReferenceQueue<V>();
    }
    
    // org.waterken.cache.Cache interface

    /**
     * Fetches a value.
     * @param otherwise The default value.
     * @param id        The identifier.
     * @return The cached value, or the default value.
     */
    public V
    fetch(final V otherwise, final K id) {
        final CacheReference<K,V> ref = entries.get(id);
        final V value = null != ref ? ref.get() : null;
        return null != value
            ? (value instanceof Null ? null : value)
            : otherwise;
    }

    /**
     * Caches a value.
     * @param id    The identifier.
     * @param value The value to cache.
     */
    public void
    put(final K id, final V value) {

        // Wipe old entries.
        while (true) {
            final Reference old = wiped.poll();
            if (null == old) { break; }
            entries.remove(((CacheReference)old).key);
        }

        entries.put(id, new CacheReference<K,V>(nonNull(value), wiped, id));
    }
    
    static private @SuppressWarnings("unchecked") <V> V
    nonNull(final V value) { return null != value ? value : (V)new Null(); }
    
    static private final class
    Null {}
}
