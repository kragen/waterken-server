// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * A <code>chunked</code> input stream.
 */
final class
ChunkedInputStream extends InputStream {

    private final InputStream in;   // underlying stream
    
    private boolean started;        // Has the first chunk been read?
    private long chunkSize;         // remaining bytes in the current chunk
    private boolean done;           // Has the last chunk been read?
    
    private boolean markedStarted;  // marked started indicator
    private long markedChunkSize;   // marked position
    private boolean markedDone;     // marked done indicator

    /**
     * Constructs an instance.
     * @param in    underlying stream
     */
    ChunkedInputStream(final InputStream in) {
        this.in = in;
    }

    /**
     * Reads in the next chunk.
     */
    private void
    _next() throws IOException {
        // Skip over the trailing CRLF from the previous chunk.
        if (started) {
            int c = in.read();
            if (c == -1) { throw new EOFException(); }
            if (c != '\r') { throw new IOException("Expected CR"); }
            c = in.read();
            if (c == -1) { throw new EOFException(); }
            if (c != '\n') { throw new IOException("Expected LF"); }
        }
        started = true;

        // Read in the chunk-size.
        int c;
        for (int i = Long.SIZE / 4; true;) {
            if (0 == --i) { throw new IOException("overflow"); }
            c = in.read();
            if (c == -1) { throw new EOFException(); }
            if (c >= '0' && c <= '9') {
                chunkSize = chunkSize * 16 + (c - '0');
            } else if (c >= 'a' && c <= 'f') {
                chunkSize = chunkSize * 16 + 10 + (c - 'a');
            } else if (c >= 'A' && c <= 'F') {
                chunkSize = chunkSize * 16 + 10 + (c - 'A');
            } else {
                break;
            }
        }

        // Skip over the chunk-extension.
        int preceding = '0';
        boolean open_quote = false;
        while (!('\r' == c && '\\' != preceding && !open_quote)) {
            preceding = c;
            c = in.read();
            if (-1 == c) { throw new EOFException(); }
            if ('\"' == c && '\\' != preceding) {
                open_quote = !open_quote;
            }
        }
        c = in.read();
        if (-1 == c) { throw new EOFException(); }
        if ('\n' != c) { throw new IOException("Expected LF"); }

        if (0 == chunkSize) {
            done = true;
            
            // Discard any trailer headers.
            final LineInput hin = new LineInput(in);
            while (!"".equals(hin.readln())) {}
        }
    }

    // java.io.InputStream interface

    public int
    read() throws IOException {
        if (done) { return -1; }
        if (0 == chunkSize) {
            _next();
            return read();
        }
        int r = in.read();
        if (r == -1) { throw new EOFException(); }
        --chunkSize;
        return r;
    }

    public int
    read(final byte[] b, int off, int len) throws IOException {
        if (done) { return -1; }
        int n = 0;
        while (len > 0) {
            if (0 == chunkSize) {
                _next();
                if (done) { break; }
            }
            final int d = in.read(b, off, (int)Math.min(len, chunkSize));
            if (d == -1) { throw new EOFException(); }
            chunkSize -= d;
            n += d;
            off += d;
            len -= d;
        }
        return n;
    }

    public long
    skip(long n) throws IOException {
        long k = 0;
        if (!done) {
            while (n > 0) {
                if (0 == chunkSize) {
                    _next();
                    if (done) { break; }
                }
                final long d = in.skip(Math.min(n, chunkSize));
                chunkSize -= d;
                k += d;
                n -= d;
            }
        }
        return k;
    }

    public int
    available() { return (int)Math.min(chunkSize, Integer.MAX_VALUE); }

    public void
    close() {}

    public void
    mark(final int readlimit) {
        in.mark(readlimit);
        markedStarted = started;
        markedChunkSize = chunkSize;
        markedDone = done;
    }

    public void
    reset() throws IOException {
        in.reset();
        started = markedStarted;
        chunkSize = markedChunkSize;
        done = markedDone;
    }

    public boolean
    markSupported() { return in.markSupported(); }
}
