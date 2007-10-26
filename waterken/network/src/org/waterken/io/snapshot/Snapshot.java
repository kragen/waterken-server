// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.snapshot;

import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.waterken.io.Content;
import org.waterken.io.stream.Stream;

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
    
    /**
     * Creates a snapshot of binary content.
     * @param estimate  estimated content length
     * @param stream    binary content to copy
     */
    static public Snapshot
    snapshot(final int estimate, final Content stream) throws Exception {
        if (stream instanceof Snapshot) { return (Snapshot)stream; }
        final ByteArray.Generator out = new ByteArray.Generator(estimate);
        stream.writeTo(out);
        return new Snapshot(out.snapshot());
    }
    
    // org.waterken.io.Content interface
    
    public void
    writeTo(final OutputStream out) throws Exception {
        Stream.copy(content.open(), out);
    }
}
