// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.joe_e.array.ConstArray;
import org.ref_send.Variable;
import org.waterken.dns.Resource;

/**
 * A {@link Resource} editor list.
 */
public interface
Section<T> {

    /**
     * Gets the editors.
     */
    ConstArray<? extends Variable<T>>
    getEntries();
    
    /**
     * Adds a {@link #getEntries resource}.
     * @return corresponding editor
     */
    Variable<T>
    add();
    
    /**
     * Removes a {@link #getEntries resource}.
     * @param editor    to be removed editor
     */
    void
    remove(Variable<T> editor);
}
