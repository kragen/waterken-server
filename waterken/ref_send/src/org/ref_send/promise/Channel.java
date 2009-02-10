// Copyright 2005-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * The reified {@linkplain Promise tail} and {@linkplain Resolver head} of a
 * reference: {@link #promise -}<code>-</code>{@link #resolver &gt;}.
 * @param <T> referent type
 */
public class
Channel<T> extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * permission to access the referent
     */
    public final Promise<T> promise;

    /**
     * permission to resolve the referent
     */
    public final Resolver<T> resolver;

    /**
     * Constructs an instance.
     * @param promise   {@link #promise}
     * @param resolver  {@link #resolver}
     */
    public @deserializer
    Channel(@name("promise") final Promise<T> promise,
            @name("resolver") final Resolver<T> resolver) {
        this.promise = promise;
        this.resolver = resolver;
    }
}
