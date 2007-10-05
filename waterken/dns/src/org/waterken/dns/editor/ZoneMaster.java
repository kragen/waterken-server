// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import java.io.Serializable;

import org.ref_send.promise.Promise;
import org.web_send.graph.Framework;
import org.web_send.graph.Host;

/**
 * A {@link Zone} implementation.
 */
public final class
ZoneMaster implements Zone, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * sub-model factory
     */
    private final Host dependent;

    private
    ZoneMaster(final Host dependent) {
        this.dependent = dependent;
    }
    
    /**
     * Constructs an instance.
     * @param framework model framework
     */
    static public Zone
    build(final Framework framework) {
        return new ZoneMaster(framework.dependent);
    }

    // org.waterken.dns.editor.Zone interface
    
    public Promise<DomainMaster>
    claim(final String hostname) {
        return dependent.share(hostname, Editor.class.getName());
    }
}
