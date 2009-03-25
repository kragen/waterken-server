// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Provides more convenient read access to a {@link ByteArrayOutputStream}.
 */
final class
ByteArrayBuilder extends ByteArrayOutputStream {

    public
    ByteArrayBuilder(final int estimate) {
        super(estimate);
    }
    
    public void
    write(final byte b[]) { write(b, 0, b.length); }
    
    public void
    write(final ByteBuffer b) {write(b.array(),b.arrayOffset(),b.remaining());}
    
    public byte
    get(final int i) { return buf[i]; }
}
