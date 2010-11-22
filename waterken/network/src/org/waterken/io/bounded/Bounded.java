// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.bounded;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A fixed length segment of a longer stream.
 */
public final class
Bounded {
    private Bounded() {}

    /**
     * Bounds an input stream segment.
     * @param length    number of bytes in the stream
     * @param in        underlying stream
     */
    static public InputStream
    input(final long length, final InputStream in) {
        if (0 > length) { throw new RuntimeException(); }
        return new InputStream() {
            private long remaining = length;    // number of bytes remaining
            private long marked = -1;           // marked position

            public int
            read() throws IOException {
                if (0L == remaining) { return -1; }
                final int r = in.read();
                if (r == -1) { throw new EOFException(); }
                --remaining;
                return r;
            }

            public int
            read(final byte[] b,final int off,final int len) throws IOException{
                if (0L == remaining) { return -1; }
                final int r = in.read(b, off, (int)Math.min(remaining, len));
                if (r == -1) { throw new EOFException(); }
                remaining -= r;
                return r;
            }

            public long
            skip(final long n) throws IOException {
                final long r = in.skip(Math.min(remaining, n));
                remaining -= (int)r;
                return r;
            }

            public int
            available() throws IOException {
                return (int)Math.min(remaining, in.available());
            }

            public void
            close() {}

            public void
            mark(final int readlimit) {
                in.mark((int)Math.min(remaining, readlimit));
                marked = remaining;
            }

            public void
            reset() throws IOException {
                if (-1 == marked) { throw new IOException(); }
                in.reset();
                remaining = marked;
            }

            public boolean
            markSupported() { return in.markSupported(); }
        };
    }
    
    /**
     * Bounds an output stream segment.
     * @param length    number of bytes in the stream
     * @param out       underlying output stream
     */
    static public OutputStream
    output(final long length, final OutputStream out) {
        if (0 > length) { throw new RuntimeException(); }
        return new OutputStream() {
            private long remaining = length;
            
            public void
            write(final int b) throws IOException {
                if (0 == remaining) { throw new EOFException(); }
                out.write(b);
                --remaining;
            }

            public void
            write(final byte[] b) throws IOException {
                if (b.length > remaining) { throw new EOFException(); }
                out.write(b);
                remaining -= b.length;
            }

            public void
            write(final byte[] b,final int off,final int len)throws IOException{
                if (len > remaining) { throw new EOFException(); }
                out.write(b, off, len);
                remaining -= len;
            }

            public void
            flush() throws IOException { out.flush(); }

            public void
            close() throws IOException {
                if (0 != remaining) { throw new EOFException(); }
            }
        };
    }
}
