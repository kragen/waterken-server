// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.inert;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.uri.Hostname;
import org.waterken.var.Guard;

/**
 * Restrictions on acceptable hostnames.
 */
public class
HostnameGuard extends Guard<String> implements Record, Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * minimum number of characters in hostname
     */
    public final int length;
    
    /**
     * required hostname prefix
     */
    public final String prefix;
    
    /**
     * required hostname suffix
     */
    public final String suffix;

    /**
     * Constructs an instance.
     * @param length    {@link #length}
     * @param prefix    {@link #prefix}
     * @param suffix    {@link #suffix}
     */
    public @deserializer
    HostnameGuard(@name("length") final int length,
                  @name("prefix") final String prefix,
                  @name("suffix") final String suffix) {
        this.length = length;
        this.prefix = prefix;
        this.suffix = suffix;
    }
    
    public @Override String
    run(@inert final String candidate) {
        if (!candidate.startsWith(prefix)) { throw new RuntimeException(); }
        if (!candidate.endsWith(suffix)) { throw new RuntimeException(); }
        if (candidate.length() < length) { throw new RuntimeException(); }
        Hostname.vet(candidate);
        return candidate;
    }
}
