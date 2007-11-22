// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

/**
 * A model maker.
 */
public interface
Host {
    
    /**
     * Creates a model.
     * <p>
     * The <code>factory</code> MUST have a method with signature:</p>
     * <p><code>static public T build({@link Framework} framework)</code>
     * </p>
     * @param <T> exported object type
     * @param factory object factory
     * @return promise for an object exported from the new model
     */
    <T> T
    spawn(Class<?> factory);

    /**
     * Creates a model with a specific label.
     * @param <T> exported object type
     * @param label     model label
     * @param factory   object factory, same requirements as in {@link #spawn}     * @param factory   object factory
     * @return promise for an object exported from the new model
     * @throws Collision    <code>label</code> has already been used
     */
    <T> T
    claim(String label, Class<?> factory) throws Collision;
}
