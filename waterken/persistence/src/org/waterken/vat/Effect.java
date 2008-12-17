// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import org.joe_e.Immutable;

/**
 * A side-effect of an {@linkplain Transaction#update update} transaction.
 */
public interface
Effect<S> extends Immutable {

    /**
     * Performs a side-effect operation.
     * @param value any additional details about the notification
     * @throws Exception    any problem
     */
    void
    run(Vat<S> value) throws Exception;    
}
