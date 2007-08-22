// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.uri;

/**
 * URI {@link URI#authority authority} manipulation. 
 */
public final class
Authority {

    private
    Authority() {}
    
    /**
     * Extracts the <code>host:port</code>.
     * @param authority URI {@link URI#authority authority}
     * @return <code>host:port</code>
     */
    static public String
    location(final String authority) {
        return authority.substring(authority.indexOf('@') + 1);
    }
}
