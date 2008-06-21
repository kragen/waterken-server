// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;


import org.waterken.uri.Header;

/**
 * Utility methods for implementing the HTTP protocol.
 */
final class
HTTP {

    private
    HTTP() {}

    static void
    vet(final String disallowed, final String value) throws Exception {
        for (int i = value.length(); 0 != i--;) {
            final char c = value.charAt(i);
            if (31 >= c || 127 <= c || disallowed.indexOf(c) != -1) {
                throw new Exception("Illegal header character");
            }
        }
    }
    
    static void
    vetToken(final String token) throws Exception {
        vet(" ()<>@,;:\\\"/[]?={}", token);
    }
    
    static void
    vetHeader(final Header header) throws Exception {
        vetToken(header.name);
        vet("", header.value);
    }
}
