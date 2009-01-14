// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.ref_send.promise.eventual.Log;
import org.waterken.db.Root;
import org.waterken.db.Vat;

/**
 * A {@link Session} maker.
 */
public final class
SessionMaker implements Serializable {
    static private final long serialVersionUID = 1L;

    private final Root local;
    
    protected
    SessionMaker(final Root local) {
        this.local = local;
    }
    
    // org.waterken.remote.http.SessionMaker interface
    
    public String
    create(final String name) {
        final Log log = local.fetch(null, Vat.log);
        return local.export(new ServerSideSession(name, log), false);
    }
}
