// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.Immutable;

/**
 * A {@link Database#enter transaction} body.
 */
public interface
Transaction<R extends Immutable> extends Immutable {
    
    /**
     * indicates a {@linkplain Database#enter transaction} may modify existing state
     */
    boolean update = false;

    /**
     * indicates a {@linkplain Database#enter transaction} only queries existing
     * state, and does not persist any new selfish objects
     */
    boolean query = true;

    /**
     * Executes the transaction.
     * @param root {@link Database} root
     * @return any return
     * @throws Exception    any problem
     */
    R run(Root root) throws Exception;
}
