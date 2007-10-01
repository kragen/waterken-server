// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.Promise;
import org.waterken.dns.Resource;
import org.waterken.dns.editor.DomainMaster;
import org.waterken.dns.editor.Zone;
import org.web_send.graph.Collision;

/**
 * A {@link Registrar} implementation.
 */
public final class
Redirectory {

    private
    Redirectory() {}

    /**
     * Constructs an instance.
     * @param suffix    hostname suffix
     * @param zone      domain factory
     */
    static public Registrar
    make(final String suffix, final Zone zone) {
        class RegistrarX extends Struct implements Registrar, Serializable {
            static private final long serialVersionUID = 1L;

            public Promise<DomainMaster<Promise<Resource>>>
            register(final ByteArray key) throws Collision {
                final String fingerprint = "";  // TODO: calc key fingerprint
                return zone.claim(fingerprint + suffix);
            }
        }
        return new RegistrarX();
    }
}
