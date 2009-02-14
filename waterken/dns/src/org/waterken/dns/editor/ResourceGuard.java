// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.joe_e.Powerless;
import org.ref_send.deserializer;
import org.waterken.dns.Resource;
import org.waterken.var.Guard;

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
    run(Resource x) {
        if (Resource.IN != x.clazz) { throw new UnsupportedClass(); }
        switch (x.type) {
        case Resource.A:
            if (x.data.length() != 4) { throw new BadFormat(); }
            if (minTTL - x.ttl > 0) {
                x = new Resource(x.type, x.clazz, minTTL, x.data);
            }
            break;
        default:
            throw new UnsupportedType();
        }
        return x;
    }
}
