// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

import org.joe_e.charset.URLEncoding;

/**
 * URI query manipulation.
 */
public final class
Query {

    private
    Query() {}

    /**
     * Extracts a query argument.
     * @param otherwise default value
     * @param query     query string
     * @param name      parameter name
     * @return unescaped argument
     */
    static public String
    arg(final String otherwise, final String query, final String name) {
        final String match = URLEncoding.encode(name);
        int start;
        if (query.startsWith(match + "=")) {
            start = match.length() + "=".length();
        } else {
            start = query.indexOf("&" + match + "=");
            if (-1 == start) { return otherwise; }
            start += "&".length() + match.length() + "=".length();
        }
        int end = query.indexOf('&', start);
        if (-1 == end) {
            end = query.length();
        }
        return URLEncoding.decode(query.substring(start, end));
    }
}
