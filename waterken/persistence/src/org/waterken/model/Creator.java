// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

/**
 * A {@link Model} factory.
 */
public interface
Creator {

    /**
     * Creates a new {@link Model}.
     * @param name          model name
     * @param initialize    first transaction to run on the new model
     * @param project       corresponding project name
     */
    <R> R
    run(String name, Transaction<R> initialize, String project);
}
