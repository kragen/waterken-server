// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

/**
 * A case-insensitive, well-known name publisher.
 */
public interface
Publisher {
    
    /**
     * set of disallowed name characters
     */
    String disallowed = ";\\/:*?<>|\"=#";

    /**
     * Creates a new binding.
     * @param name  name to bind
     * @param value value to bind
     * @throws Collision    <code>name</code> is unavailable
     */
    void
    bind(String name, Object value) throws Collision;

    /**
     * Creates a named vat.
     * @param <T> exported object type
     * @param name  vat name
     * @param maker object maker, same requirements as in {@link Spawn#run}
     * @return promise for the object returned by the <code>maker</code>
     * @throws Collision    <code>name</code> is unavailable
     */
    <T> T
    spawn(String name, Class<?> maker) throws Collision;
}
