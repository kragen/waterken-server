// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

/**
 * A case-insensitive name to value mapping.
 */
public interface
Namespace {
    
    /**
     * set of disallowed name characters
     */
    String disallowed = ".;\\/:*?<>|\"=#";

    /**
     * Retrieves a value.
     * @param otherwise default value
     * @param name      name to lookup
     * @return bound value, or <code>otherwise</code>
     */
    Object
    use(Object otherwise, String name);

    /**
     * Creates a new binding.
     * @param name  name to bind
     * @param value value to bind
     * @throws Collision    <code>name</code> was already bound
     */
    void
    bind(String name, Object value) throws Collision;
}
