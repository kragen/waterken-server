// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.limited;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.waterken.io.Content;
import org.web_send.Failure;

/**
 * A stream that is not allowed to be longer than a preset limit.
 */
public final class
Limited {

    private
    Limited() {}

    /**
     * Limits an input stream.
     * @param max   maximum number of bytes that can be read
     * @param in    underlying stream
     */
    static public InputStream
    input(final long max, final InputStream in) {
        if (0 > max) { throw new RuntimeException(); }
        return new InputStream() {
            private long remaining = max;   // number of bytes remaining 
            private long marked = max;      // marked position

            public int
            read() throws IOException {
                if (0L == remaining) { throw Failure.tooBig(); }
                final int r = in.read();
                if (r != -1) { --remaining; }
                return r;
            }

            public int
            read(final byte[] b,final int off,final int len) throws IOException{
                if (0L == remaining) { throw Failure.tooBig(); }
                final int n = in.read(b, off, (int)Math.min(remaining, len));
                if (n != -1) { remaining -= n; }
                return n;
            }

            public long
            skip(final long n) throws IOException {
                final long r = in.skip(Math.min(remaining, n));
                remaining -= r;
                return r;
            }

            public int
            available() throws IOException {
                return (int)Math.min(remaining, in.available());
            }

            public void
            close() throws IOException { in.close(); }

            public void
            mark(final int readlimit) {
                in.mark((int)Math.min(remaining, readlimit));
                marked = remaining;
            }

            public void
            reset() throws IOException {
                in.reset();
                remaining = marked;
            }

            public boolean
            markSupported() { return in.markSupported(); }
        };
    }
    
    /**
     * Limits an output stream.
     * @param max   maximum number of bytes that can be written
     * @param out   underlying output stream
     */
    static public OutputStream
    output(final long max, final OutputStream out) {
        if (0 > max) { throw new RuntimeException(); }
        return new OutputStream() {
            private long remaining = max;   // number of bytes remaining 
            
            public void
            write(final int b) throws IOException {
                if (0 == remaining) { throw Failure.tooBig(); }
                out.write(b);
                --remaining;
            }

            public void
            write(final byte[] b) throws IOException {
                if (b.length > remaining) { throw Failure.tooBig(); }
                out.write(b);
                remaining -= b.length;
            }

            public void
            write(final byte[] b,final int off,final int len)throws IOException{
                if (len > remaining) { throw Failure.tooBig(); }
                out.write(b, off, len);
                remaining -= len;
            }

            public void
            flush() throws IOException { out.flush(); }

            public void
            close() throws IOException { out.close(); }
        };
    }
    
    /**
     * Limits a content source.
     * @param max       maximum number of bytes allowed
     * @param untrusted content source
     */
    static public Content
    limit(final long max, final Content untrusted) {
        class ContentX extends Struct implements Content, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            writeTo(final OutputStream out) throws Exception {
                untrusted.writeTo(output(max, out));
            }
        }
        return new ContentX();
    }
}
