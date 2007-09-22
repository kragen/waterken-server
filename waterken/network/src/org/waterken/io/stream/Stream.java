// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.joe_e.Struct;
import org.waterken.io.Content;

/**
 * Stream content.
 */
public final class
Stream extends Struct implements Content {

    /**
     * byte stream
     */
    public final InputStream bytes;
    
    /**
     * Constructs an instance.
     * @param bytes {@link #bytes}
     */
    public
    Stream(final InputStream bytes) {
        this.bytes = bytes;
    }
    
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
    
    // org.waterken.io.Content interface
    
    public void
    writeTo(final OutputStream out) throws IOException { copy(bytes, out); }
}
