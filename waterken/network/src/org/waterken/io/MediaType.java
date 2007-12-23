// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.joe_e.array.PowerlessArray;
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
     * <code>application/json</code> MIME type
     */
    static public final MediaType json =
        new MediaType(".json",      "application/json",                 null);

    /**
     * <code>text/uri-list</code> MIME type
     */
    static public final MediaType uri =
        new MediaType(".uri",       "text/uri-list; charset=US-ASCII",  null);

    /**
     * known MIME types
     */
    static public final PowerlessArray<MediaType> MIME = PowerlessArray.array(
        uri,
        json,
        new MediaType(".html.gz",   "text/html; charset=ISO-8859-1",    gzip),
        new MediaType(".html",      "text/html; charset=ISO-8859-1",    null),
        new MediaType(".css.gz",    "text/css; charset=ISO-8859-1",     gzip),
        new MediaType(".css",       "text/css; charset=ISO-8859-1",     null),
        new MediaType(".js.gz",     "application/javascript",           gzip),
        new MediaType(".js",        "application/javascript",           null),
        new MediaType(".htm.gz",    "text/html; charset=ISO-8859-1",    gzip),
        new MediaType(".htm",       "text/html; charset=ISO-8859-1",    null),
        new MediaType(".swf",       "application/x-shockwave-flash",    null),
        new MediaType(".xml",       "application/xml",                  null),
        new MediaType(".xsl",       "application/xml",                  null),
        new MediaType(".txt",       "text/plain; charset=US-ASCII",     null),
        new MediaType(".rdf",       "application/rdf+xml",              null),
        new MediaType(".gif",       "image/gif",                        null),
        new MediaType(".png",       "image/png",                        null),
        new MediaType(".jpg",       "image/jpeg",                       null),
        new MediaType(".ico",       "image/vnd.microsoft.icon",         null),
        binary
    );

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
            r = (null != ext ? ext.equals(x.ext) : null == x.ext) &&
                (null != name ? name.equals(x.name) : null == x.name) &&
                (null!=encoding ?encoding.equals(x.encoding) :null==x.encoding);
        }
        return r;
    }

    /**
     * Calculates the hash code.
     */
    public int
    hashCode() {
        return 0x313E719E +
               (null != ext ? ext.hashCode() : 0) +
               (null != name ? name.hashCode() : 0) +
               (null != encoding ? encoding.hashCode() : 0);
    }
}
