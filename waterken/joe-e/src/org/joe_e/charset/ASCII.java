// Copyright 2006-2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.charset;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 * ASCII I/O.
 */
public final class ASCII {
    private ASCII() {}

    /**
     * Encodes a string in US-ASCII.
     * @param text  The text to encode.
     * @return The ASCII bytes.
     */
    static public byte[] encode(final String text) {
        try {
            return text.getBytes("US-ASCII");
        } catch (final UnsupportedEncodingException e) {
            // This should never happen, as US-ASCII is a required encoding
            throw new AssertionError("US-ASCII encoding not supported");
        }
    }
    
    /**
     * Constructs an ASCII reader.
     * @param in    The binary input stream
     * @return The ASCII character reader.
     */
    static public Reader input(final InputStream in) {
        try {
            return new InputStreamReader(in, "US-ASCII");
        } catch (final UnsupportedEncodingException e) {
            // This should never happen, as US-ASCII is a required encoding
            throw new AssertionError("US-ASCII encoding not supported");
        }
    }

    /**
     * Constructs an ASCII writer.
     * @param out   The output stream.
     */
    static public Writer output(final OutputStream out) {
        try {
            return new OutputStreamWriter(out, "US-ASCII");
        } catch (final UnsupportedEncodingException e) {
            // This should never happen, as US-ASCII is a required encoding
            throw new AssertionError("US-ASCII encoding not supported");
        }
    }
}
