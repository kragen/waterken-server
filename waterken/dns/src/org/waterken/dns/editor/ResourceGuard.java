// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.joe_e.Powerless;
import org.joe_e.array.ByteArray;
import org.ref_send.deserializer;
import org.waterken.dns.Resource;
import org.waterken.var.Guard;

/**
 * Conditions on allowed DNS resources.
 */
public final class
ResourceGuard extends Guard<ByteArray> implements Powerless {
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
    
    public @Override ByteArray
    run(ByteArray x) {
        if (Resource.IN != Resource.clazz(x)) { throw new UnsupportedClass(); }
        switch (Resource.type(x)) {
        case Resource.A:
            if (Resource.length(x) != 4) { throw new BadFormat(); }
            if (2 + 2 + 4 + 2 + 4 != x.length()) { throw new BadFormat(); }
            if (minTTL - Resource.ttl(x) > 0) {
                x = Resource.rr(Resource.type(x), Resource.clazz(x), minTTL,
                                Resource.data(x).toByteArray());
            }
            break;
        default:
            throw new UnsupportedType();
        }
        return x;
    }
}
