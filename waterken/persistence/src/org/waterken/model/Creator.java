// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

import org.web_send.graph.Collision;

/**
 * A {@link Model} factory.
 */
public interface
Creator {
    
    /**
     * Loads a project's class library.
     * @param project   project name
     * @return corresponding class library
     */
    ClassLoader
    load(String project) throws Exception;
    
    /**
     * Generates a unique random label.
     */
    String
    generate();

    /**
     * Creates a new {@link Model}.
     * @param project       corresponding project name
     * @param label         model label
     * @param initialize    first transaction to run on the new model
     * @throws NoLabelReuse <code>label</code> has already been used
     */
    <R> R
    create(String project, String label,
           Transaction<R> initialize) throws Collision;
}
