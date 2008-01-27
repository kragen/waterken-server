// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * MIME type information.
 */
public class
MediaType implements Powerless, Selfless, Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * GZIP {@link #encoding} name 
     */
    static public final String gzip = "gzip";

    /**
     * <code>application/octet-stream</code> MIME type
     */
    static public final MediaType binary =
        new MediaType("",           "application/octet-stream",         null);
    
    /**
     * <code>text/uri-list</code> MIME type
     */
    static public final MediaType uri =
        new MediaType(".uri",       "text/uri-list; charset=US-ASCII",  null);

    /**
     * filename extension
     */
    public final String ext;

    /**
     * Media Type
     */
    public final String name;
    
    /**
     * Content-Encoding
     */
    public final String encoding;

    /**
     * Constructs an instance.
     * @param ext       {@link #ext}
     * @param name      {@link #name}
     * @param encoding  {@link #encoding}
     */
    public @deserializer
    MediaType(@name("ext") final String ext,
              @name("name") final String name,
              @name("encoding") final String encoding) {
        if (null == ext) { throw new NullPointerException(); }
        if (null == name) { throw new NullPointerException(); }
        
        this.ext = ext;
        this.name = name;
        this.encoding = encoding;
    }

    // java.lang.Object interface

    /**
     * Is the given object the same?
     * @param o compared to object
     * @return true if the same, else false
     */
    public boolean
    equals(final Object o) {
        boolean r = null != o && getClass() == o.getClass();
        if (r) {
            final MediaType x = (MediaType)o;
            r = ext.equals(x.ext) && name.equals(x.name) &&
                (null!=encoding ?encoding.equals(x.encoding) :null==x.encoding);
        }
        return r;
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() {
        return 0x313E719E + ext.hashCode() + name.hashCode() +
               (null != encoding ? encoding.hashCode() : 0);
    }
}
