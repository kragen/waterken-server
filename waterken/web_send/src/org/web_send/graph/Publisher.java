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
     * @throws Collision    <code>name</code> has already been used
     */
    void
    bind(String name, Object value) throws Collision;

    /**
     * Creates a named model.
     * @param <T> exported object type
     * @param name      model name
     * @param factory   object maker, same requirements as in {@link Spawn#run}
     * @return promise for an object exported from the new model
     * @throws Collision    <code>name</code> has already been used
     */
    <T> T
    spawn(String name, Class<?> factory) throws Collision;
}
