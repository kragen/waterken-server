// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;

import org.joe_e.Token;
import org.ref_send.promise.eventual.Log;
import org.waterken.vat.Root;
import org.waterken.vat.Vat;
import org.web_send.session.Session;

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
    
    public Session
    create() {
        final Log log = local.fetch(null, Vat.log);
        final String name = local.export(new Token(), false);
        final String key = local.export(new ServerSideSession(name,log), false);
        return new Session(name, key);
    }
}
