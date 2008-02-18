// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Promise;
import org.waterken.uri.Hostname;
import org.web_send.graph.Framework;
import org.web_send.graph.Publisher;

/**
 * A {@link Registrar} implementation.
 */
public final class
RegistrarMaker {
    
    private
    RegistrarMaker() {}
    
    /**
     * Constructs an instance.
     * @param framework vat permissions
     */
    static public Registrar
    build(final Framework framework) { return make(framework.publisher); }

    /**
     * Constructs an instance.
     * @param publisher sub-vat factory
     */
    static public Registrar
    make(final Publisher publisher) {
        class RegistrarX extends Struct implements Registrar, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Promise<DomainMaster>
            claim(final String hostname) {
                Hostname.vet(hostname);
                return publisher.spawn(hostname, DomainMaker.class);
            }
        }
        return new RegistrarX();
    }
}
