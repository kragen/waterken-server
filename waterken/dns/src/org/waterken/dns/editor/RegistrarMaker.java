// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Vat;
import org.waterken.dns.Resource;
import org.waterken.menu.Menu;
import org.waterken.menu.MenuMaker;
import org.waterken.uri.Hostname;
import org.waterken.var.Guard;

/**
 * A {@link Registrar} implementation.
 */
public final class
RegistrarMaker {
    private RegistrarMaker() {}

    static public final int maxEntries = 8;
    static public final ByteArray localhost = Resource.rr(
        Resource.A,Resource.IN, ResourceGuard.minTTL, new byte[] { 127,0,0,1 });
    
    /**
     * Constructs an instance.
     * @param _     eventual operator
     * @param guard hostname guard (optional)
     */
    static public Registrar
    make(final Eventual _, final Guard<String> guard) {
        class RegistrarX extends Struct implements Registrar, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Guard<String>
            getHostnameGuard() { return guard; }
            
            public Promise<Vat<Menu<ByteArray>>>
            claim(final String hostname) throws RuntimeException {
                final Vat<Menu<ByteArray>> r = _.spawn(
                    null!=guard ? guard.run(hostname) : Hostname.vet(hostname),
                    MenuMaker.class, maxEntries, localhost,new ResourceGuard()); 
                return ref(r);
            }
        }
        return new RegistrarX();
    }
}
