// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.joe_e.array.ConstArray;
import org.ref_send.Variable;
import org.ref_send.promise.Promise;
import org.waterken.dns.Resource;

/**
 * A {@link Resource} editor list.
 */
public interface
Section {

    /**
     * Gets the editors.
     */
    ConstArray<? extends Variable<? extends Promise<Resource>>>
    getEntries();
    
    /**
     * Adds a {@link #getEntries resource}.
     * @return corresponding editor
     */
    Variable<? extends Promise<Resource>>
    add();
    
    /**
     * Removes a {@link #getEntries resource}.
     * @param editor    to be removed editor
     */
    void
    remove(Variable<? extends Promise<Resource>> editor);
}
