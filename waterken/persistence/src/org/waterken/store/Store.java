// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

/**
 * A <code>byte</code> storage interface.
 */
public interface
Store {
    
    /**
     * Recursively deletes all contained entries. 
     * @throws Exception                any I/O problem
     */
    void clean() throws Exception;
    
    /**
     * Creates a transaction.
     * <p>
     * Only one transaction can be active at a time.
     * </p>
     * @throws Exception                any I/O problem
     */
    Update update() throws Exception;
}
