// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.waterken.dns.editor.Registrar;
import org.waterken.dns.editor.RegistrarMaker;

/**
 * A fingerprint registrar.
 */
public final class
RedirectoryMaker {
    private RedirectoryMaker() {}
    
    /**
     * Return from {@link #make Redirectory.make()}.
     */
    static public class
    Return extends Struct implements Record, Serializable {
        static private final long serialVersionUID = 1L;
        
        /**
         * corresponding {@link RedirectoryMaker}
         */
        public final Registrar redirectory;
        
        /**
         * a {@link RegistrarMaker}
         */
        public final Registrar registrar;
        
        /**
         * Constructs an instance.
         * @param redirectory   {@link #redirectory}
         * @param registrar     {@link #registrar}
         */
        public @deserializer
        Return(@name("redirectory") final Registrar redirectory,
               @name("registrar") final Registrar registrar) {
            this.redirectory = redirectory;
            this.registrar = registrar;
        }
    }
    
    /**
     * Constructs a ( redirectory, registrar ) pair.
     * @param _         eventual operator
     * @param prefix    {@link Redirectory#prefix}
     * @param suffix    {@link Redirectory#suffix}
     */
    static public Promise<Return>
    make(final Eventual _, final String prefix, final String suffix) {
        return ref(new Return(
            new Redirectory(_, prefix, suffix),
            RegistrarMaker.make(_)));
    }
}
