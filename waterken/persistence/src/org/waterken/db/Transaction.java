// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.Immutable;
import org.joe_e.Struct;

/**
 * A {@link Database#enter transaction} body.
 */
public abstract class
Transaction<R extends Immutable> extends Struct implements Immutable {
    
    /**
     * indicates a {@linkplain Database#enter transaction} may modify existing state
     */
    static public final boolean update = false;

    /**
     * indicates a {@linkplain Database#enter transaction} only queries existing
     * state, and does not persist any new selfish objects
     */
    static public final boolean query = true;
    
    /**
     * either {@link #update} or {@link #query} 
     */
    public final boolean isQuery;
    
    /**
     * Constructs an instance.
     * @param isQuery   {@link #isQuery}
     */
    protected
    Transaction(final boolean isQuery) {
        this.isQuery = isQuery;
    }

    /**
     * Executes the transaction.
     * @param local {@link Database} root
     * @return any return
     * @throws Exception    any problem
     */
    public abstract R
    run(Root local) throws Exception;
}
