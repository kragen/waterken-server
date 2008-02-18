// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.joe_e.Powerless;
import org.ref_send.deserializer;
import org.ref_send.var.Guard;
import org.waterken.dns.Resource;

/**
 * Conditions on allowed DNS resources.
 */
public final class
ResourceGuard extends Guard<Resource> implements Powerless {
    static private final long serialVersionUID = 1L;
    
    /**
     * minimum {@link Resource#ttl}
     */
    static public final int minTTL = 60;

    /**
     * Constructs an instance.
     */
    public @deserializer
    ResourceGuard() {}
    
    // org.ref_send.var.Guard interface
    
    public @Override Resource
    run(final Resource value) {
        if (Resource.IN != value.clazz) { throw new UnsupportedClass(); }
        switch (value.type) {
        case Resource.A:
            if (minTTL - value.ttl > 0) { throw new UseLongerTTL(minTTL); }
            if (value.data.length() != 4) { throw new BadFormat(); }
            break;
        default:
            throw new UnsupportedType();
        }
        return value;
    }
}
