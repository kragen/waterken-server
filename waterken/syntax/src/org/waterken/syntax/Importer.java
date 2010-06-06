// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.lang.reflect.Type;

/**
 * A reference importer.
 */
public interface
Importer {

    /**
     * Imports a reference.
     * @param href  reference identifier
     * @param base  base URL, may be <code>null</code>
     * @param type  types implemented by the referenced object
     * @return corresponding reference
     * @throws Exception    any problem connecting to the identified reference
     */
    Object apply(String href, String base, Type... type) throws Exception;
}
