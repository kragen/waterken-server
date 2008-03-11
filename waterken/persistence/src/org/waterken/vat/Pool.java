// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.vat;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * A pool of open vats.
 */
public interface
Pool {

    /**
     * Connects to an existing vat.
     * @param id    vat identifier
     * @throws FileNotFoundException    vat does not exist
     */
    Vat
    connect(File id) throws Exception;
}
