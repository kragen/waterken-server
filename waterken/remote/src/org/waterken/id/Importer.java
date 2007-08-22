// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.id;

/**
 * A reference importer.
 */
public interface
Importer {

    /**
     * Imports a reference.
     * @param type  reference type
     * @param URL   reference identifier
     * @return corresponding reference
     */
    Object
    run(Class<?> type, String URL);
}
