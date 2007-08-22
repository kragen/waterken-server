// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.waterken.io.Content;

/**
 * Buffered binary data.
 */
public final class
Buffer implements Content, java.io.Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * number of bytes
     */
    public final long length;

    /**
     * each byte chunk
     * <p>
     * This member is a list of byte arrays. From index zero up to the end of
     * the list are byte[] references. From the end of the list to the end of
     * the byte[][] are null pointers. The sum of the length of each contained
     * byte[] is equal to the {@link #length}.
     * </p>
     */
    private final byte[][] chunk;

    private
    Buffer(final long length, final byte[][] chunk) {
        this.length = length;
        this.chunk = chunk;
    }

    /**
     * Constructs an instance.
     * @param value bytes
     */
    static public Buffer
    wrap(final byte[] value) {
        return new Buffer(value.length, new byte[][] { value });
    }

    /**
     * Buffers an input stream.
     * @param in    stream
     * @throws IOException  I/O problem
     */
    static public Buffer
    read(final InputStream in) throws IOException {
        long length = 0;
        byte[][] value = new byte[8][];
        for (int i = 0; true; ++i) {
            if (value.length == i) {
                System.arraycopy(value, 0, value = new byte[2 * i][], 0, i);
                // Throws a NegativeArraySizeException if 2 * i wraps.
                // This means the buffer is limited to 1.3 TB of data.
            }
            final byte[] b = value[i] = new byte[chunkSize];
            for (int j = 0; j != chunkSize;) {
                final int c = in.read(b, j, chunkSize - j);
                if (-1 == c) {
                    in.close();
                    return new Buffer(length, value);
                }
                j += c;
                length += c;
            }
        }
    }

    /**
     * Copies content.
     * @param in    content to copy
     * @throws Exception  any exception
     */
    static public Buffer
    copy(final Content in) throws Exception {
        class Copy extends OutputStream {

            private byte[][] value; // 8 <= value.length
            private int i;          // 0 <= i < value.length
            private int j;          // 0 <= j < chunkSize == value[i].length

            Copy() {
                value = new byte[8][];
                value[0] = new byte[chunkSize];
                i = 0;
                j = 0;
            }

            private void
            grow() {
                if (chunkSize == j) {
                    j = 0;
                    ++i;
                    if (value.length == i) {
                        System.arraycopy(value,0,value=new byte[2*i][],0,i);
                        // Throws a NegativeArraySizeException if 2 * i wraps.
                        // This means the buffer is limited to 1.3 TB of data.
                    }
                    value[i] = new byte[chunkSize];
                }
            }

            // java.io.OutputStream interface

            public void
            close() {}

            public void
            flush() {}

            public void
            write(final byte[] b) { write(b, 0, b.length); }

            public void
            write(final byte[] b, int off, int len) {
                if (0 > off || 0 > len) {
                    throw new IndexOutOfBoundsException();
                }
                while (0 != len) {
                    final int n = Math.min(chunkSize - j, len);
                    System.arraycopy(b, off, value[i], j, n);
                    off += n;
                    len -= n;
                    j += n;
                    grow();
                }
            }

            public void
            write(final int b) {
                value[i][j++] = (byte)b;
                grow();
            }

            // Copy interface

            Buffer
            run() throws Exception {
                in.writeTo(this);
                final byte[][] tmp = value;
                value = null;
                return new Buffer(((long)chunkSize) * i + j, tmp);
            }
        }
        return new Copy().run();
    }

    // org.waterken.io.Content interface

    public void
    writeTo(final OutputStream out) throws IOException {
        long n = length;
        for (int i = 0; 0 != n; ++i) {
            final byte[] b = chunk[i];
            final int len = n > b.length ? b.length : (int)n;
            out.write(b, 0, len);
            n -= len;
        }
    }
    
    // org.waterken.io.buffer.Buffer interface

    public InputStream
    open() {
        return new InputStream() {

            private long remaining = length;
            private int i = 0;      // 0 <= i < chunk.length
            private int j = 0;      // 0 <= j < chunk[i].length

            private long markRemaining = remaining;
            private int markI = i;
            private int markJ = j;

            // java.io.InputStream interface

            public int
            available() { return (int)Math.min(remaining, Integer.MAX_VALUE); }

            public void
            close() {}

            public boolean
            markSupported() { return true; }

            public void
            mark(final int readlimit) {
                markRemaining = remaining;
                markI = i;
                markJ = j;
            }

            public void
            reset() {
                remaining = markRemaining;
                i = markI;
                j = markJ;
            }

            public int
            read() {
                if (0 == remaining) {
                    return -1;
                }
                int r = chunk[i][j++] & 0x00FF;
                if (chunk[i].length == j) {
                    ++i;
                    j = 0;
                }
                --remaining;
                return r;
            }

            public int
            read(final byte[] b) { return read(b, 0, b.length); }

            public int
            read(final byte[] b, int off, final int len) {
                if (0 > off || 0 > len) {
                    throw new IndexOutOfBoundsException();
                }
                if (0 == remaining) {
                    return -1;
                }
                int r = 0;
                while (len != r && 0 != remaining) {
                    final int end = j + remaining > chunk[i].length
                        ? chunk[i].length : j + (int)remaining;
                    final int c = Math.min(len - r, end - j);
                    System.arraycopy(chunk[i], j, b, off, c);
                    r += c;
                    off += c;
                    remaining -= c;
                    j += c;
                    if (chunk[i].length == j) {
                        ++i;
                        j = 0;
                    }
                }
                return r;
            }

            public long
            skip(final long n) {
                long r = 0;
                while (n > r && 0 != remaining) {
                    final int end = j + remaining > chunk[i].length
                        ? chunk[i].length : j + (int)remaining;
                    final int c = (int)Math.min(n - r, end - j);
                    r += c;
                    remaining -= c;
                    j += c;
                    if (chunk[i].length == j) {
                        ++i;
                        j = 0;
                    }
                }
                return r;
            }
        };
    }
}
