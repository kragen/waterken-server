// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

/**
 * A model maker.
 * @see org.web_send.graph.Publisher#spawn
 */
public interface
Spawn {
    
    /**
     * Creates a model.
     * <p>
     * The <code>factory</code> MUST have a method with signature:</p>
     * <p><code>static public T build({@link Framework} framework)</code>
     * </p>
     * @param <T> exported object type
     * @param factory object maker
     * @return promise for an object exported from the new model
     */
    <T> T
    run(Class<?> factory);
}
