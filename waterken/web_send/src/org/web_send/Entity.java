// Copyright 2004-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Selfless;
import org.joe_e.array.ByteArray;

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
	 * raw data Media Type: {@value}
	 * <p>
	 * Web user agents, like <a
	 * href="http://msdn.microsoft.com/en-us/library/ms775147.aspx">IE6</a>,
	 * will Content-Type sniff a received entity to find HTML to execute.
	 * Javascript code inside this sniffed HTML content can then script other
	 * frames from the same origin. Consequently, an application that thought it
	 * was just providing some bytes for download, may actually be making itself
	 * vulnerable to a Cross-Site-Scripting (XSS) attack. Currently, the only
	 * known way to avoid triggering this web user agent "feature" is to use a
	 * Content-Type that the web user agent does not recognize.
	 * </p>
	 * <p>
	 * At some future time, this class may be extended to support Media-Types
	 * that do get sniffed, by checking that this sniffing is safe.
	 * </p>
	 */
    static public final String doNotExecute = "application/do-not-execute"; 

    /**
     * canonicalized Media Type
     */
    public final String type;

    /**
     * binary content
     */
    public final ByteArray content;

    /**
     * Constructs an instance.
     * @param type      {@link #type}, MUST be {@link #doNotExecute}
     * @param content   {@link #content}
     * @throws Failure  <code>content</code> bigger than {@link #maxContentSize}
     */
    public
    Entity(final String type, final ByteArray content) throws Failure {
        if (!doNotExecute.equals(type)) { throw Failure.notSupported(); }
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
