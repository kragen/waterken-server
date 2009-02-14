// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.file.InvalidFilenameException;

/**
 * The roots of a persistent object graph.
 * <p>
 * A {@link Root} provides administrative authority over a {@link Database}. By
 * convention, {@link Database} infrastructure code keeps hidden state in this
 * mapping by prefixing the binding name with a '<code>.</code>' character.
 * </p>
 */
public interface
Root {

    /**
     * Retrieves a stored value.
     * @param otherwise default value
     * @param name      name to lookup
     * @return stored value, or <code>otherwise</code>
     */
    <T> T fetch(Object otherwise, String name);

    /**
     * Creates a symbolic link to a given value.
     * <p>
     * A name assigned via this method does not affect the value returned
     * by {@link #export export}().
     * </p>
     * @param name  name to bind
     * @param value value to store
     * @throws InvalidFilenameException <code>name</code> is already bound
     * @throws ProhibitedModification   in a {@link Transaction#query}
     */
    void link(String name, Object value) throws InvalidFilenameException,
                                                ProhibitedModification;
    
    /**
     * Assigns a name to a given value.
     * @param value     value to store
     * @param isWeak    <code>false</code> if garbage collection of the
     *                  <code>value</code> is prevented, else <code>true</code>
     * @return assigned name
     * @throws ProhibitedCreation       in a {@link Transaction#query}
     */
    String export(Object value, boolean isWeak) throws ProhibitedCreation;
    // TODO: change interface to be @inert value, and no isWeak argument.
    // follow with a link call
}
