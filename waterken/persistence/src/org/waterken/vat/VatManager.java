// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import java.io.File;

/**
 * A {@link Vat} connection manager.
 */
public interface
VatManager<S> {

    /**
     * Gets the connection to a vat.
     * @param id    vat identifier
     * @return corresponding vat
     * @throws Exception    any problem
     */
    Vat<S> connect(File id) throws Exception;
}
