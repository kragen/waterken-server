// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.ref_send.promise.Promise;
import org.waterken.uri.Hostname;
import org.web_send.graph.Framework;
import org.web_send.graph.Publisher;

/**
 * A {@link Zone} implementation.
 */
public final class
ZoneMaster implements Zone, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * sub-model factory
     */
    private final Publisher publisher;

    private
    ZoneMaster(final Publisher publisher) {
        this.publisher = publisher;
    }
    
    /**
     * Constructs an instance.
     * @param framework model framework
     */
    static public Zone
    build(final Framework framework) {
        return new ZoneMaster(framework.publisher);
    }

    // org.waterken.dns.editor.Zone interface
    
    public Promise<DomainMaster>
    claim(final String hostname) {
        Hostname.vet(hostname);
        return publisher.spawn(hostname, Editor.class);
    }
}
