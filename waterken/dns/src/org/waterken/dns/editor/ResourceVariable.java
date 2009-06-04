package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.Receiver;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;

public final class
ResourceVariable implements Receiver<ByteArray>, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * minimum {@link Resource#ttl}
     */
    static public final int minTTL = 60;
    
    /**
     * default value of a {@linkplain Menu#grow created} {@link Resource}
     */
    static public final ByteArray prototype = Resource.rr(Resource.A,
        Resource.IN, ResourceVariable.minTTL, new byte[] { 127,0,0,1 });
    
    private ByteArray value;
    
    protected
    ResourceVariable() {
        value = prototype;
    }
    
    public ByteArray
    get() { return value; }

    public void
    apply(ByteArray rr) {
        if (Resource.IN != Resource.clazz(rr)) { throw new UnsupportedClass(); }
        switch (Resource.type(rr)) {
        case Resource.A:
            if (Resource.length(rr) != 4) { throw new BadFormat(); }
            if (2 + 2 + 4 + 2 + 4 != rr.length()) { throw new BadFormat(); }
            if (minTTL - Resource.ttl(rr) > 0) {
                rr = Resource.rr(Resource.type(rr), Resource.clazz(rr), minTTL,
                                 Resource.data(rr).toByteArray());
            }
            break;
        default:
            throw new UnsupportedType();
        }
        this.value = rr;
    }
}
