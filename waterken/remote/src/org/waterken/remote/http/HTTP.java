// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.charset.URLEncoding;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.http.Server;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remote;
import org.waterken.uri.URI;

/**
 * Dispatches to the corresponding peer specific messenger.
 */
/* package */ final class
HTTP extends Struct implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final Eventual _;
    private final Receiver<Effect<Server>> effect;
    private final Fulfilled<Outbound> outbound;
    private final Root local;
    // TODO: keep a list here instead of in the root.
    
    protected
    HTTP(final Eventual _, final Receiver<Effect<Server>> effect,
         final Fulfilled<Outbound> outbound, final Root local) {
        this._ = _;
        this.effect = effect;
        this.outbound = outbound;
        this.local = local;
    }
    
    // org.waterken.remote.Messenger interface

    public <R> R
    when(final String href, final Remote proxy,
         final Class<?> R, final Do<Object,R> observer) {
        return Exports.isPromise(URI.fragment("", href))
            ? message(href).when(href, proxy, R, observer)
        : proxy.fulfill(R, observer);
    }
    
    public Object
    invoke(final String href, final Object proxy,
           final Method method, final Object... arg) {
        return message(href).invoke(href, proxy, method, arg);
    }
    
    private Messenger
    message(final String href) {
        final String peer = URLEncoding.encode(URI.resolve(href, "."));
        final String peerKey = ".peer-" + peer;
        Pipeline msgs = local.fetch(null, peerKey);
        if (null == msgs) {
            final String name = local.export(new Token(), false);
            msgs = new Pipeline(name, peer, effect, outbound);
            local.link(peerKey, msgs);
        }
        final Exports exports = local.fetch(null, VatInitializer.exports);
        return new Caller(_, msgs, exports);
    }
}
