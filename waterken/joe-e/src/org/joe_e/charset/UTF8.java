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
 * UTF-8 I/O.
 */
public final class UTF8 {
    private UTF8() {}
    
    /**
     * Encodes a string in UTF-8.
     * @param text  The text to encode.
     * @return The UTF-8 bytes.
     */
    static public byte[] encode(final String text) {
        try {
            return text.getBytes("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // This should never happen, as UTF-8 is a required encoding
            throw new AssertionError("UTF-8 encoding not supported");
        }
    }
    
    /**
     * Constructs a UTF-8 reader.
     * @param in    The binary input stream.
     */
    static public Reader input(final InputStream in) {
        try {
            return new InputStreamReader(in, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // This should never happen, as UTF-8 is a required encoding
            throw new AssertionError("UTF-8 encoding not supported");
        }
    }
    
    /**
     * Constructs a UTF-8 writer.
     * @param out   The binary output stream.
     */
    static public Writer output(final OutputStream out) {
        try {
            return new OutputStreamWriter(out, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            // This should never happen, as UTF-8 is a required encoding
            throw new AssertionError("UTF-8 encoding not supported");
        }
    }
}
