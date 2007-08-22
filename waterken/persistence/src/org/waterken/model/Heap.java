// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

/**
 * An address space.
 */
public interface
Heap {

    /**
     * Retrieves an object by address.
     * @param address   object address
     * @return object at the given address
     * @throws NullPointerException no object at the given address
     */
    Object
    reference(long address) throws NullPointerException;

    /**
     * Locates an object.
     * @param object    object to locate
     * @return object's address
     */
    long
    locate(Object object);
}
