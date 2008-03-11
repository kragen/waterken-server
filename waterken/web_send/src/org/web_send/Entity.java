// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;

/**
 * A MIME entity.
 */
public final class
Entity implements Powerless, Selfless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * maximum number of bytes in {@link #content}: {@value}
     */
    static public final int maxContentSize = 256 * 1024;
    
    /**
     * binary Media Type: {@value}
     */
    static public final String binary = "application/octet-stream"; 
    
    /**
     * supported Media Types
     * <p>
     * Some Media Types, such as HTML, are automatically provided with
     * additional permissions by the web browser, such as the permission to
     * script other browser frames from the same origin. Consequently, it is not
     * always safe to serve such content. This field lists the known safe and
     * so supported Media Types.
     * </p>
     */
    static public final PowerlessArray<String> supported = PowerlessArray.array(
        binary,
        "text/plain"
    );
    
    /**
     * Is a given Media Type supported?
     * @param type  Media Type
     */
    static public boolean
    supports(final String type) {
        final int c = type.indexOf(';');
        final String m = -1 == c ? type : type.substring(0, c);
        for (final String x : supported) {
            if (x.equals(m)) { return true; }
        }
        return false;
    }

    /**
     * Media Type
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
     * @throws Failure  <code>content</code> bigger than {@link #maxContentSize}
     */
    public
    Entity(final String type, final ByteArray content) throws Failure {
        if (!supports(type)) { throw Failure.notSupported(); }
        if (maxContentSize < content.length()) { throw Failure.tooBig(); }
        
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
        return o instanceof Entity &&
               type.equals(((Entity)o).type) &&
               content.equals(((Entity)o).content);
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0x313E817E + type.hashCode() + content.hashCode(); }
}
