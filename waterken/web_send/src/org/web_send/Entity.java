// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.Promise;

/**
 * A MIME entity.
 */
public class
Entity implements Promise<Entity>, Powerless, Selfless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * MIME Media Type
     */
    public final String type;

    /**
     * binary content
     */
    public final ByteArray content;

    /**
     * Constructs an instance.
     * @param type      {@link #type}
     * @param content   {@link #content}
     */
    public
    Entity(final String type, final ByteArray content) {
        this.type = type;
        this.content = content;
    }

    // java.lang.Object interface.

    /**
     * Is the given object the same?
     * @param o compared to object
     * @return <code>true</code> if the same, else <code>false</code>
     */
    public boolean
    equals(final Object o) {
        return null != o && getClass() == o.getClass() &&
               (null != type
                ? type.equals(((Entity)o).type)
                : null == ((Entity)o).type) &&
               (null != content
                ? content.equals(((Entity)o).content)
                : null == ((Entity)o).content);
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() {
        return 0x313E817E +
               (null != type ? type.hashCode() : 0) +
               (null != content ? content.hashCode() : 0);
    }
    
    // org.ref_send.promise.Volatile interface

    /**
     * @return <code>this</code>
     */
    public Entity
    cast() { return this; }
}
