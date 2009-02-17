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

/**
 * A {@link Registrar} implementation.
 */
public final class
RegistrarMaker {
    private RegistrarMaker() {}

    static public final int maxEntries = 8;
    static public final ByteArray localhost= Resource.rr(
        Resource.A,Resource.IN, ResourceGuard.minTTL, new byte[] { 127,0,0,1 });
    static public final ResourceGuard guard = new ResourceGuard();
    
    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    static public Registrar
    make(final Eventual _) {
        class RegistrarX extends Struct implements Registrar, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Promise<Vat<Menu<ByteArray>>>
            claim(final String hostname) throws RuntimeException {
                Hostname.vet(hostname);
                final Vat<Menu<ByteArray>> r = _.spawn(hostname,
                        MenuMaker.class, maxEntries, localhost, guard); 
                return ref(r);
            }
        }
        return new RegistrarX();
    }
}
