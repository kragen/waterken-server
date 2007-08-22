// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;

/**
 * A memory sensitive cache.
 */
final class
Cache<E> implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * The object cache: [ id =&gt; soft value ]
     */
    private transient HashMap<String,CacheReference<E>> entries;

    /**
     * The cache exit queue.
     */
    private transient ReferenceQueue<E> wiped;

    /**
     * Constructs an instance.
     */
    Cache() {
        entries = new HashMap<String,CacheReference<E>>();
        wiped = new ReferenceQueue<E>();
    }

    /**
     * Restores the transient data structures.
     */
    private void
    readObject(final ObjectInputStream in) throws IOException,
                                                  ClassNotFoundException {
        // Read in the attributes.
        in.defaultReadObject();

        // Setup the cache structures.
        entries = new HashMap<String,CacheReference<E>>();
        wiped = new ReferenceQueue<E>();
    }

    /**
     * Fetches a value.
     * @param otherwise The default value.
     * @param id        The identifier.
     * @return The cached value, or the default value.
     */
    E
    fetch(final E otherwise, final String id) {
        final CacheReference<E> ref = entries.get(id);
        final E value = null != ref ? ref.get() : null;
        return null != value
            ? (value instanceof Null ? null : value)
            : otherwise;
    }

    /**
     * Caches a value.
     * @param id    The identifier.
     * @param value The value to cache.
     */
    @SuppressWarnings("unchecked") void
    put(final String id, final E value) {

        // Wipe old entries.
        while (true) {
            final Reference old = wiped.poll();
            if (null == old) { break; }
            entries.remove(((CacheReference)old).key);
        }

        entries.put(id, new CacheReference<E>(
            null != value ? value : (E)new Null(), wiped, id));
    }
    
    static private final class
    Null {}
}
