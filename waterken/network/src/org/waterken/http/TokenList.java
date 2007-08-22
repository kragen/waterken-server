// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

/**
 * Parses a <code>token</code> list.
 */
public final class
TokenList {

    private
    TokenList() {}

    /**
     * Encodes a list of <code>token</code>.
     * @param token each <code>token</code>
     * @return encoded <code>token</code> list
     */
    static public String
    encode(final String... token) {
        final StringBuilder buffer = new StringBuilder();
        for (int i = 0; i != token.length; ++i) {
            if (i != 0) { buffer.append(", "); }
            buffer.append(token[i]);
        }
        return buffer.toString();
    }

    /**
     * Decodes a <code>token</code> list.
     * @param list  <code>token</code> list
     * @return <code>token</code> array
     */
    static public String[]
    decode(final String list) {
        String[] r = new String[1];
        int n = 0;
        final int len = list.length();
        for (int i = 0; true; ++i) {
            // Eat whitespace.
            while (i != len && " \t\r\n".indexOf(list.charAt(i)) != -1) { ++i; }

            // Empty list permitted.
            if (i == len) { break; }

            // Null elements permitted.
            if (',' == list.charAt(i)) { continue; }

            // Parse the token.
            int startToken = i;
            while (i != len && " ,;\t\r\n".indexOf(list.charAt(i)) == -1) {++i;}
            if (n == r.length) { System.arraycopy(r,0,r=new String[2*n],0,n); }
            r[n++] = list.substring(startToken, i);

            // Discard the parameters.
            while (true) {
                // Eat whitespace.
                while (i!=len && " \t\r\n".indexOf(list.charAt(i)) != -1) {++i;}

                // Check for token delimiter.
                if (i == len || ',' == list.charAt(i)) { break; }

                // Start parameter.
                if (';' != list.charAt(i)) { throw new RuntimeException(); }
                ++i;

                // Eat whitespace.
                while (" \t\r\n".indexOf(list.charAt(i)) != -1) { ++i; }

                // Discard the name.
                while ("= \t\r\n".indexOf(list.charAt(i)) == -1) { ++i; }

                // Start the value.
                if ('=' != list.charAt(i)) { throw new RuntimeException(); }
                ++i;

                // Discard the value.
                if ('\"' == list.charAt(i)) {
                    while (true) {
                        final char c = list.charAt(++i);
                        if ('\"' == c) { break;}
                        if ('\\' == c) { ++i; }
                    }
                    ++i;
                } else {
                    while (i != len &&
                           " ,\t\r\n".indexOf(list.charAt(i)) == -1) { ++i; }
                }
            }

            if (i == len) { break; }
        }
        if (n != r.length) { System.arraycopy(r, 0, r = new String[n], 0, n); }
        return r;
    }
}
