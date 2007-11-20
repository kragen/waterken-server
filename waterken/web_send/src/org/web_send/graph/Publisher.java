// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

/**
 * A case-insensitive well-known name publisher.
 */
public interface
Publisher {
    
    /**
     * set of disallowed name characters
     */
    String disallowed = ".;\\/:*?<>|\"=#";

    /**
     * Creates a new binding.
     * @param name  name to bind
     * @param value value to bind
     * @throws Collision    <code>name</code> was already bound
     */
    void
    run(String name, Object value) throws Collision;
}
