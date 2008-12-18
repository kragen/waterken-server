// Copyright 2004-2005 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * URI location manipulation.
 */
public final class
Location {

    private
    Location() {}

    /**
     * Extracts the <code>host</code>.
     * @param location  URI {@link Authority#location location}
     * @return <code>host</code>
     */
    static public String
    hostname(final String location) {
        final int c = location.indexOf(':');
        return Header.toLowerCase(-1 == c ? location : location.substring(0,c));
    }

    /**
     * Extracts the <code>port</code>.
     * @param standard  standard port number
     * @param location  URL location
     * @return <code>port</code>
     */
    static public int
    port(final int standard, final String location) {
        final int c = location.indexOf(':');
        return -1 == c || location.length() == c + 1
            ? standard
        : Integer.parseInt(location.substring(c + 1));
    }
}
