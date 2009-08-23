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
     * Executes the transaction.
     * @param root {@link Database} root
     * @return any return
     * @throws Exception    any problem
     */
    R apply(Root root) throws Exception;
}
