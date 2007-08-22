// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A rejected promise.
 * @param <T> referent type
 */
public final class
Rejected<T> implements Promise<T>, Powerless, Selfless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * reason for rejecting the promise
     */
    public final Exception reason;

    /**
     * Construct an instance.
     * @param reason    {@link #reason}
     */
    public @deserializer
    Rejected(@name("reason") final Exception reason) {
        this.reason = reason;
    }
    
    // java.lang.Object interface
    
    /**
     * Is the given object the same?
     * @param x compared to object
     * @return <code>true</code> if the same, else <code>false</code>
     */
    public boolean
    equals(final Object x) {
        return x instanceof Rejected &&
               (null != reason
                   ? reason.equals(((Rejected)x).reason)
                   : null == ((Rejected)x).reason);
    }
    
    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0xDEADBEA7; }

    // org.ref_send.promise.Volatile interface

    /**
     * Throws the {@link #reason}.
     * @throws  Exception   {@link #reason}
     */
    public T
    cast() throws Exception { throw reason; }
}
