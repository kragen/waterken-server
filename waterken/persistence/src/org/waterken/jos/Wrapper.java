// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.Serializable;

import org.waterken.db.Root;

/**
 * A serialization wrapper object. 
 */
interface
Wrapper extends Serializable {

    /**
     * Peel off the serialization wrapper.
     * @param root  vat root
     * @return unwrapped object
     */
    Object
    peel(Root root) throws IOException;
}
