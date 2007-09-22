// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.snapshot;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.waterken.io.Content;

/**
 * Fixed binary content.
 */
public final class
Snapshot extends Struct implements Content, Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * binary content
     */
    public final ByteArray content;
    
    /**
     * Constructs an instance.
     * @param content   {@link #content}
     */
    public
    Snapshot(final ByteArray content) {
        this.content = content;
    }
    
    // org.waterken.io.Content interface

    // TODO: suggest changes to ByteArray interface to reduce copying
    
    public void
    writeTo(final OutputStream out) throws Exception {
        out.write(content.toByteArray());
    }
    
    // org.waterken.io.snapshot.Snapshot interface
    
    /**
     * Creates a snapshot of binary content.
     */
    static public ByteArray
    copy(final Content stream) throws Exception {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        stream.writeTo(out);
        return ByteArray.array(out.toByteArray());
    }
}
