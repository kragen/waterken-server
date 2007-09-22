// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.EOFException;
import java.io.InputStream;
import java.io.IOException;

import org.joe_e.charset.ASCII;

/**
 * An HTTP line reader.
 * <p>
 * This implementation follows the guidelines set out in section 19.3 of
 * RFC 2616.
 * </p>
 */
final class
LineInput {

    private final InputStream in;   // underlying stream
    private byte[] buffer;          // line buffer

    /**
     * Constructs an instance.
     * @param in    underlying stream
     */
    LineInput(final InputStream in) {
        this.in = in;
        buffer = new byte[256];
    }

    // org.waterken.net.http.LineInput interface

    /**
     * Reads a line of text.
     * @throws IOException  I/O problem
     */
    String
    readln() throws IOException {

        // Read until LF.
        int len = 0;
        while (true) {
            final int c = in.read();
            if (c == -1) { throw new EOFException(); }
            if (c == '\n') { break; }
            if (len == buffer.length) {
                System.arraycopy(buffer, 0, buffer = new byte[2 * len], 0, len);
            }
            buffer[len] = (byte)c;
            ++len;
        }

        // Strip off any trailing CR.
        while (len != 0 && buffer[len - 1] == '\r') { --len; }

        return ASCII.decode(buffer, 0, len);
    }
}
