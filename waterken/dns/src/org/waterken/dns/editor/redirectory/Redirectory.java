// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor.redirectory;

import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Vat;
import org.waterken.dns.editor.HostMaker;
import org.waterken.dns.editor.Registrar;
import org.waterken.menu.Menu;
import org.waterken.uri.Hostname;

/**
 * A fingerprint registrar.
 */
public final class
Redirectory extends Struct implements Registrar, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * eventual operator
     */
    private final Eventual _;
    
    /**
     * required prefix on hostnames
     */
    public final String prefix;
    
    /**
     * required suffix on hostnames
     */
    public final String suffix;
    
    /**
     * Constructs an instance.
     * @param _         eventual operator
     * @param prefix    {@link #prefix}
     * @param suffix    {@link #suffix}
     */
    public
    Redirectory(final Eventual _, final String prefix, final String suffix) {
        this._ = _;
        this.prefix = prefix;
        this.suffix = suffix;
    }
    
    public Promise<Vat<Menu<ByteArray>>>
    claim(final String hostname) throws RuntimeException {
        final int minChars = prefix.length() + (80 / 5)      + suffix.length();
        final int maxChars = prefix.length() + (128 / 5 + 1) + suffix.length();
        
        if (hostname.length() < minChars) { throw new RuntimeException(); }
        if (hostname.length() > maxChars) { throw new RuntimeException(); }
        if (!hostname.startsWith(prefix)) { throw new RuntimeException(); }
        if (!hostname.endsWith(suffix))   { throw new RuntimeException(); }
        final Vat<Menu<ByteArray>> r = _.spawn(Hostname.vet(hostname),
                                               HostMaker.class); 
        return ref(r);
    }
}
