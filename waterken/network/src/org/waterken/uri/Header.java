// Copyright 2004-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.Record;

/**
 * A named string parameter.
 */
public final class
Header implements Powerless, Selfless, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * name
     */
    public final String name;

    /**
     * value
     */
    public final String value;

    /**
     * Constructs an instance.
     * @param name  {@link #name}
     * @param value {@link #value}
     */
    public @deserializer
    Header(@name("name") final String name,
           @name("value") final String value) {
        this.name = name;
        this.value = value;
    }

    // java.lang.Object interface

    /**
     * Is the given object the same?
     * @param o compared to object
     * @return true if the same, else false
     */
    public boolean
    equals(final Object o) {
        if (!(o instanceof Header)) { return false; }
        final Header x = (Header)o;
        return (null != name ? name.equals(x.name) : null == x.name) &&
               (null != value ? value.equals(x.value) : null == x.value);
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() {
        return (null != name ? name.hashCode() : 0) +
               (null != value ? value.hashCode() : 0);
    }
    
    // org.waterken.uri.Header interface

    /**
     * Finds the first instance of the named header.
     * @param otherwise default value
     * @param headers   each header
     * @param name      searched for header name
     * @return found header value, or the <code>otherwise</code> if not found
     */
    static public String
    find(final String otherwise,
         final Iterable<Header> headers, final String name) {
        for (final Header h : headers) {
            if (name.equalsIgnoreCase(h.name)) { return h.value; }
        }
        return otherwise;
    }
}
