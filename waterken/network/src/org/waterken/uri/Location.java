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
        final int end_host = location.indexOf(':');
        return -1 == end_host ? location : location.substring(0, end_host);
    }

    /**
     * Extracts the <code>port</code>.
     * @param standard  standard port number
     * @param location  URL location
     * @return <code>port</code>
     */
    static public int
    port(final int standard, final String location) {
        final int end_host = location.indexOf(':');
        return -1 == end_host || location.length() == end_host + 1
            ? standard
            : Integer.parseInt(location.substring(end_host + 1));
    }
}
