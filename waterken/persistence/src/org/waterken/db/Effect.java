// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import org.joe_e.Immutable;

/**
 * A side-effect of an {@linkplain Database#update update} transaction.
 */
public interface
Effect<S> extends Immutable {

    /**
     * Performs a side-effect operation.
     * @param origin    vat that produced this side-effect
     * @throws Exception    any problem
     */
    void apply(Database<S> origin) throws Exception;    
}
