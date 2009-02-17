// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Vat;
import org.waterken.dns.editor.Registrar;
import org.waterken.menu.Menu;

/**
 * A fingerprint registrar.
 */
public final class
Redirectory {
    private Redirectory() {}
    
    /**
     * Constructs a ( registrar, redirectory ) pair.
     * @param prefix    required prefix on redirectory hostnames
     * @param suffix    required suffix on redirectory hostnames
     * @param registrar underlying registrar
     */
    static public Registrar
    make(final String prefix, final String suffix, final Registrar registrar) {
        final int minChars = prefix.length() + (80 / 5) + suffix.length();
        class RegistrarX extends Struct implements Registrar, Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<Vat<Menu<ByteArray>>>
            claim(final String hostname) throws RuntimeException {
                if (!hostname.startsWith(prefix)){throw new RuntimeException();}
                if (!hostname.endsWith(suffix)) { throw new RuntimeException();}
                if (hostname.length() < minChars){throw new RuntimeException();}
                return registrar.claim(hostname);
            }
        }
        return new RegistrarX();
    }
}
