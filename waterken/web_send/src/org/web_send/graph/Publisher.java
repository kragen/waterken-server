// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

import org.joe_e.Struct;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.promise.Promise;

/**
 * A case-insensitive, well-known name publisher.
 */
public abstract class
Publisher extends Struct {
    
    /**
     * set of disallowed name characters: {@value}
     */
    static public final String disallowed = ";\\/:*?<>|\"=#";

    /**
     * Creates a new binding.
     * @param name  name to bind
     * @param value value to bind
     * @throws InvalidFilenameException <code>name</code> not available
     */
    public abstract void
    bind(String name, Object value) throws InvalidFilenameException;

    /**
     * Creates a named vat.
     * @param <R> return type, MUST be either an interface, or a {@link Promise}
     * @param name      vat name, or <code>null</code> for a generated name
     * @param builder   same requirements as in {@link Spawn#run}
     * @param argv      more arguments for <code>builder</code>'s build method
     * @return promise for the object returned by the <code>builder</code>
     * @see org.web_send.graph.Spawn#run
     */
    public abstract <R> R
    spawn(String name, Class<?> builder, Object... argv);
}
