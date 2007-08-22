// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * Base32 encoding.
 */
public final class
Base32 {

    private
    Base32() {}

    // org.waterken.uri.Base32 interface

    /**
     * Encodes binary data in base32.
     * @param bytes binary data
     * @return base32 encoding
     */
    static public String
    encode(byte[] bytes) {
        final StringBuilder r = new StringBuilder(bytes.length * 8 / 5 + 1);
        int buffer = 0;
        int bufferSize = 0;
        for (final byte b : bytes) {
            buffer <<= 8;
            buffer |= b & 0x000000FF;
            bufferSize += 8;
            while (bufferSize >= 5) {
                bufferSize -= 5;
                r.append(at((buffer >>> bufferSize) & 0x1F));
            }
        }
        if (0 != bufferSize) {
            buffer <<= 5 - bufferSize;
            r.append(at(buffer & 0x1F));
        }        
        return r.toString();
    }

    static private char
    at(final int v) { return (char)(v < 26 ? v + 'a' : v - 26 + '2'); }

    /**
     * Decodes base32 data to binary.
     * @param chars base32 data
     * @return decoded binary data
     * @throws InvalidBase32    decoding error
     */
    static public byte[]
    decode(final String chars) throws InvalidBase32 {
        final byte[] r = new byte[chars.length() * 5 / 8];
        int buffer = 0;
        int bufferSize = 0;
        int j = 0;
        for (int i = 0; i != chars.length(); ++i) {
            buffer <<= 5;
            buffer |= locate(chars.charAt(i));
            bufferSize += 5;
            if (bufferSize >= 8) {
                bufferSize -= 8;
                r[j++] = (byte)(buffer >>> bufferSize);
            }
        }
        if (0 != (buffer & ((1 << bufferSize) - 1))) {
            throw new InvalidBase32();
        }
        return r;
    }

    static private int
    locate(final char c) throws InvalidBase32 {
        return 'a' <= c && 'z' >= c
            ? c - 'a'
        : ('A' <= c && 'Z' >= c
            ? c - 'A'
        : ('2' <= c && '7' >= c
            ? 26 + (c - '2')
        : invalid()));
    }

    static private int
    invalid() throws InvalidBase32 { throw new InvalidBase32(); }
}
