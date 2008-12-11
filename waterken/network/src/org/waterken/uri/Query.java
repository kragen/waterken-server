// Copyright 2004-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

import java.util.Iterator;
import java.util.NoSuchElementException;

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
        int start;
        if (query.startsWith(name + "=")) {
            start = name.length() + "=".length();
        } else {
            start = query.indexOf("&" + name + "=");
            if (-1 == start) { return otherwise; }
            start += "&".length() + name.length() + "=".length();
        }
        int end = query.indexOf('&', start);
        if (-1 == end) {
            end = query.length();
        }
        return URLEncoding.decode(query.substring(start, end));
    }
    
    /**
     * Parses a query string.
     * @param query query string
     * @return ( name, value ) pair sequence
     */
    static public Iterable<Header>
    parse(final String query) {
        return new Iterable<Header>() {
            public Iterator<Header>
            iterator() {
                final int len = query.length();
                return new Iterator<Header>() {
                    private int i = 0;

                    public boolean
                    hasNext() { return len == i; }

                    public Header
                    next() {
                        if (len == i) { throw new NoSuchElementException(); }

                        // parse the name
                        final int beginName = i;
                        int endName = i;
                        while (len != i) {
                            final char c = query.charAt(i++);
                            if ('=' == c) { break; }
                            ++endName;
                        }
                        final String name = URLEncoding.decode(
                            query.substring(beginName, endName));
                        
                        // parse the value
                        final int beginValue = i;
                        int endValue = i;
                        while (len != i) {
                            final char c = query.charAt(i++);
                            if ('&' == c) { break; }
                            ++endValue;
                        }
                        final String value = URLEncoding.decode(
                            query.substring(beginValue, endValue));
                        
                        return new Header(name, value);
                    }

                    public void
                    remove() { throw new UnsupportedOperationException(); }
                };
            }
        };
    }
}
