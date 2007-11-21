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

    /**
     * Checks a <code>location</code> for invalid characters.
     * @param location  candidate location
     * @throws InvalidLocation  rejected <code>location</code>
     */
    static public void
    vet(final String location) throws InvalidLocation {
        for (int i = location.length(); 0 != i--;) {
            final char c = location.charAt(i);
            if (!(URI.unreserved(c) || URI.subdelim(c) ||
                  ":[]%".indexOf(c) != -1)) { throw new InvalidLocation(); }
        }
    }
}
