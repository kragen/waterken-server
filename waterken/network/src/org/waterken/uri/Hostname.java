// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * A DNS hostname.
 */
public final class
Hostname {

    private
    Hostname() {}
    
    /**
     * Checks a hostname for invalid characters.
     * @param hostname  hostname to vet
     */
    static public String
    vet(final String hostname) throws InvalidLabel {
        for (int j = hostname.length(); -1 != j;) {
            final int dot = hostname.lastIndexOf('.', j - 1); 
            Label.vet(hostname, dot + 1, j);
            j = dot;
        }
        return hostname;
    }
}
