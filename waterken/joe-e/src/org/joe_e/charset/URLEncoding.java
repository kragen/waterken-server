// Copyright 2006-2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.charset;

import java.net.URLEncoder;
import java.net.URLDecoder;
import java.io.UnsupportedEncodingException;

/**
 * Class for converting strings to and from the 
 * <code>application/x-www-form-urlencoded</code> MIME format used for HTML
 * forms.  Uses the UTF-8 character encoding, as specified by W3C.  This class
 * contains static methods for converting strings between human-readable text 
 * form and its corresponding encoding.
 */
public class URLEncoding {
    private URLEncoding() {}
    
    /**
     * URL encode a value.
     * @param value The value to encode.
     * @return The encoded URL segment.
     */
    static public String encode(final String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 encoding not supported"); // Should never happen
        }
    }

    /**
     * URL decode a segment.
     * @param segment   The segment to decode.
     * @return The decoded value.
     */
    static public String decode(final String segment) {
        try {
            return URLDecoder.decode(segment, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new AssertionError("UTF-8 encoding not supported"); // Should never happen
        }
    }
}
