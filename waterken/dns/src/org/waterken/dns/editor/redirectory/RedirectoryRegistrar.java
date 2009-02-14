// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import static org.ref_send.promise.Eventual.ref;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.waterken.dns.editor.Registrar;
import org.waterken.dns.editor.RegistrarMaker;

/**
 * Constructs a ( redirectory, registrar ) pair.
 */
public final class
RedirectoryRegistrar {
    private RedirectoryRegistrar() {}
    
    /**
     * Constructs a ( redirectory, registrar ) pair.
     * @param _         eventual operator
     * @param prefix    required prefix on redirectory hostnames
     * @param suffix    required suffix on redirectory hostnames
     */
    static public Promise<ConstArray<Registrar>>
    make(final Eventual _, final String prefix, final String suffix) {
        final Registrar registrar = RegistrarMaker.make(_);
        return ref(ConstArray.array(
            Redirectory.make(prefix, suffix, registrar), registrar));
    }
}
