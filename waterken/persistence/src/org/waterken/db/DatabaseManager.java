// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

import java.io.File;

/**
 * A {@link Database} connection manager.
 */
public interface
DatabaseManager<S> {

    /**
     * Gets the connection to a vat.
     * @param id    vat identifier
     * @return corresponding vat
     * @throws Exception    any problem
     */
    Database<S> connect(File id) throws Exception;
}
