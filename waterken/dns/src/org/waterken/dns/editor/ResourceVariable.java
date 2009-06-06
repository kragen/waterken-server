package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.Receiver;
import org.waterken.dns.Resource;

/* package */ final class
ResourceVariable implements Receiver<ByteArray>, Serializable {
    static private final long serialVersionUID = 1L;
    
    private ByteArray value;
    
    protected
    ResourceVariable() {}
    
    protected ByteArray
    get() { return value; }

    public void
    apply(ByteArray rr) {
        if (Resource.IN != Resource.clazz(rr)) { throw new UnsupportedClass(); }
        switch (Resource.type(rr)) {
        case Resource.A:
            if (Resource.length(rr) != 4) { throw new UnsupportedType(); }
            if (Resource.headerLength+4 != rr.length()) {throw new BadFormat();}
            if (Resource.minTTL - Resource.ttl(rr) > 0) {
                rr = Resource.rr(Resource.type(rr), Resource.clazz(rr),
                        Resource.minTTL, Resource.data(rr).toByteArray());
            }
            break;
        default:
            throw new UnsupportedType();
        }
        this.value = rr;
    }
}
