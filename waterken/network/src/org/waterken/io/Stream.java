// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joe_e.array.ByteArray;

/**
 * Stream content.
 */
public final class
Stream {
    
    /**
     * preferred chunk size for writing to an output stream
     */
    static public final int chunkSize = 1280;

    private
    Stream() {}
    
    /**
     * Copies bytes from a source stream to a destination stream.
     * @param in    input stream
     * @param out   output stream
     * @throws IOException  any I/O problem
     */
    static public void
    copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[chunkSize];
        while (true) {
            final int n = in.read(buffer);
            if (-1 == n) { break; }
            out.write(buffer, 0, n);
        }
    }

    /**
     * Creates a snapshot of binary content.
     * @param estimate  estimated content length
     * @param stream    binary content to copy
     * @throws IOException  any I/O problem
     */
    static public ByteArray
    snapshot(final int estimate, final InputStream stream) throws IOException {
        final ByteArray.Builder out = ByteArray.builder(estimate);
        copy(stream, out.asOutputStream());
        return out.snapshot();
    }}
