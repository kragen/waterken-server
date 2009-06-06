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
import org.waterken.menu.Menu;
import org.waterken.uri.Hostname;
import org.waterken.uri.InvalidLabel;

/**
 * A {@link Registrar} implementation.
 */
public final class
RegistrarMaker {
    private RegistrarMaker() {}
    
    /**
     * Constructs an instance.
     * @param _         eventual operator
     * @param suffix    required suffix on a hostname
     * @param prefix    required prefix on a hostname
     */
    static public Registrar
    make(final Eventual _, final String suffix, final String prefix) {
        class RegistrarX extends Struct implements Registrar, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Zone
            getZone() { return new Zone(suffix, prefix); }
            
            public Promise<Vat<Menu<ByteArray>>>
            claim(final String hostname) throws RuntimeException {
                if (!hostname.endsWith(suffix))   { throw new InvalidLabel();}
                if (!hostname.startsWith(prefix)) { throw new InvalidLabel(); }
                
                final Vat<Menu<ByteArray>> r =
                    _.spawn(Hostname.vet(hostname), HostMaker.class, hostname); 
                return ref(r);
            }

            public Registrar
            restrict(final String tld, final String sub) throws InvalidLabel {
                if (!tld.endsWith(suffix))   { throw new InvalidLabel(); }
                if (!sub.startsWith(prefix)) { throw new InvalidLabel(); }

                return make(_, tld, sub);
            }
        }
        return new RegistrarX();
    }
}
