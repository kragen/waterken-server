// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

/**
 * Adapts the {@link ByteBuffer} API to the {@link InputStream} API.
 */
final class
ByteBufferInputStream extends InputStream {
    
    /**
     * underlying buffer
     */
    private final ByteBuffer buffer;
    
    /**
     * Constructs an instance.
     * @param buffer    underlying buffer
     */
    public
    ByteBufferInputStream(final ByteBuffer buffer) {
        this.buffer = buffer;
    }
    
    // java.io.InputStream interface
    
    public int
    read() {
        try {
            return 0xFF & buffer.get();
        } catch (final BufferUnderflowException e) { return -1; }
    }
    
    public int
    read(final byte b[]) { return read(b, 0, b.length); }

    public int
    read(final byte[] b, final int off, final int len) {
        final int remaining = buffer.remaining();
        if (0 == remaining) { return -1; }
        final int r = Math.min(len, remaining);
        buffer.get(b, off, r);
        return r;
    }

    public long
    skip(final long n) {
        final int r = (int)Math.min(n, buffer.remaining());
        buffer.position(buffer.position() + r);
        return r;
    }

    public int
    available() { return buffer.remaining(); }

    public void
    close() {}

    public boolean
    markSupported() { return true; }

    public void
    mark(final int readlimit) { buffer.mark(); }

    public void
    reset() throws IOException { buffer.reset(); }
}
