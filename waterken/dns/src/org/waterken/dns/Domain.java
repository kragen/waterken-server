// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns;

import org.joe_e.array.ConstArray;

/**
 * The public information about a hostname.
 */
public interface
Domain {
    
    /**
     * exported name
     */
    String name = "public";

    /**
     * Gets the standard query response.
     */
    ConstArray<Resource>
    getAnswers();
}
