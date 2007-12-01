// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html

package org.waterken.dns.editor;

import java.io.Serializable;

import org.ref_send.Variable;
import org.waterken.dns.Resource;

/**
 * A {@link Resource} slot.
 */
public final class
ResourceSlot implements Variable<Resource>, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * minimum {@link Resource#ttl}
     */
    static public final int minTTL = 60;

    /**
     * current value
     */
    private Resource value;
    
    // org.ref_send.Variable interface
    
    public Resource
    get() { return value; }
    
    public void
    put(final Resource value) throws Disallowed {
        // vet the assigned value
        if (Resource.IN != value.clazz) { throw new Disallowed(); }
        switch (value.type) {
        case Resource.A:
            if (value.ttl < minTTL) { throw new Disallowed(); }
            if (value.data.length() != 4) { throw new Disallowed(); }
            break;
        default:
            throw new Disallowed();
        }

        this.value = value;
    }
}
