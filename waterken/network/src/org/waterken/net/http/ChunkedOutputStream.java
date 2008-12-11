// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.IOException;
import java.io.OutputStream;

import org.joe_e.inert;

/**
 * A <code>chunked</code> output stream.
 */
final class
ChunkedOutputStream extends OutputStream {

    private final OutputStream out; // underlying stream

    private final byte[] buffer;    // data output buffer
    private final int last;         // end of data buffer position
    private final int first;        // start of data buffer position

    private int i;                  // current data buffer position
    private boolean closed;         // Is the stream closed?

    /**
     * Constructs an instance.
     * @param size  buffer size
     * @param out   underlying stream
     */
    ChunkedOutputStream(final int size, @inert final OutputStream out) {
        this.out = out;

        buffer = new byte[size];
        last = size - "\r\n0\r\n\r\n".length();
        first = Integer.toHexString(last).length() + "\r\n".length();

        i = first;
        closed = false;
    }

    public void
    write(final int b) throws IOException {
        if (closed) { throw new IOException(); }
        if (last == i) { _flush(false); }
        buffer[i++] = (byte)b;
    }

    public void
    write(final byte[] b, int off, int len) throws IOException {
        if (closed) { throw new IOException(); }
        while (0 != len) {
            if (last == i) { _flush(false); }
            final int n = Math.min(len, last - i);
            System.arraycopy(b, off, buffer, i, n);
            i += n;
            off += n;
            len -= n;
        }
    }

    public void
    flush() throws IOException {
        if (!closed) {
            if (first != i) { _flush(false); }
            out.flush();
        }
    }

    public void
    close() throws IOException {
        if (!closed) {
            closed = true;
            _flush(true);
        }
    }

    private void
    _flush(final boolean done) throws IOException {

        final int n = i - first;
        final String pre = Integer.toHexString(n) + "\r\n";
        int off = first;
        for (int j = pre.length(); j-- != 0;) {
            buffer[--off] = (byte)pre.charAt(j);
        }
        buffer[i++] = '\r';
        buffer[i++] = '\n';
        if (done && 0 != n) {
            buffer[i++] = '0';
            buffer[i++] = '\r';
            buffer[i++] = '\n';
            buffer[i++] = '\r';
            buffer[i++] = '\n';
        }
        out.write(buffer, off, i - off);
        i = first;
    }
}
