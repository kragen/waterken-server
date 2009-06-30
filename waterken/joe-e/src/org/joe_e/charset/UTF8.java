// Copyright 2006-2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.charset;

import java.nio.charset.Charset;
import java.nio.ByteBuffer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

/**
 * UTF-8 I/O.
 */
public final class UTF8 {
    private static final Charset charset = Charset.forName("UTF-8");
    
    private UTF8() {}
    
    /**
     * Encodes a string in UTF-8.
     * @param text  The text to encode.
     * @return The UTF-8 bytes.
     */
    static public byte[] encode(final String text) {
        final ByteBuffer bytes = charset.encode(text);
        final int len = bytes.limit();
        final byte[] v = bytes.array();
        if (len == v.length) { return v; }
        final byte[] r = new byte[len];
        System.arraycopy(v, bytes.arrayOffset(), r, 0, len);
        return r;
    }
    
    /**
     * Decodes a UTF-8 string. Each byte not corresponding to a UTF-8
     * character decodes to the Unicode replacement character U+FFFD.
     * Note that an initial byte-order mark is not stripped.  This method is
     * equivalent to <code>decode(buffer, 0, buffer.length)</code>.
     * @param buffer    the ASCII-encoded string to decode
     * @return The corresponding string
     * @throws java.lang.IndexOutOfBoundsException
     */
    static public String decode(byte[] buffer) {
        return decode(buffer, 0, buffer.length);
    }
    
    /**
     * Decodes a UTF-8 string. Each byte not corresponding to a UTF-8
     * character decodes to the Unicode replacement character U+FFFD.
     * Note that an initial byte-order mark is not stripped.
     * @param buffer    the ASCII-encoded string to decode
     * @param off       where to start decoding
     * @param len       how many bytes to decode
     * @return The corresponding string
     * @throws java.lang.IndexOutOfBoundsException
     */
    static public String decode(byte[] buffer, int off, int len) {
        return charset.decode(ByteBuffer.wrap(buffer, off, len)).toString();
    }
    
    /**
     * Constructs a UTF-8 reader.
     * @param in    The binary input stream.
     */
    static public Reader input(final InputStream in) {
        return new InputStreamReader(in, charset);
    }
    
    /**
     * Constructs a UTF-8 writer.
     * @param out   The binary output stream.
     */
    static public Writer output(final OutputStream out) {
        return new OutputStreamWriter(out, charset);
    }
}
