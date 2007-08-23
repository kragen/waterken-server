// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

import org.joe_e.Struct;
import org.joe_e.charset.URLEncoding;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.ref_send.type.Typedef;
import org.waterken.id.exports.Exports;
import org.waterken.model.Root;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.uri.Authority;
import org.waterken.uri.Location;
import org.waterken.uri.URI;

/**
 * Dispatches to the corresponding peer specific messenger.
 */
public final class
HTTP extends Struct implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final String scheme;
    private final int standardPort;
    private final Root local;
    
    public
    HTTP(final String scheme, final int standardPort, final Root local) {
        this.scheme = scheme;
        this.standardPort = standardPort;
        this.local = local;
    }
    
    // org.waterken.remote.Messenger interface

    /**
     * {@link Do} block parameter type
     */
    static private final TypeVariable DoP = Typedef.name(Do.class, "P");

    @SuppressWarnings("unchecked") public <P,R> R
    when(final String URL, final Class<?> R, final Do<P,R> observer) {
        final String src = Exports.src(URL);
        if (null != src) { return message(src).when(src, R, observer); }
        final Eventual _ = (Eventual)local.fetch(null, Remoting._);
        final R r;
        final Do<P,?> forwarder;
        if (void.class == R || Void.class == R) {
            r = null;
            forwarder = observer;
        } else {
            final Channel<R> x = _.defer();
            r = R.isAssignableFrom(Promise.class)
                    ? (R)x.promise : _.cast(R, x.promise);
            forwarder = _.compose(observer, x.resolver);
        }
        final Class<?> P = Typedef.raw(Typedef.value(DoP, observer.getClass()));
        final P a = (P)Remote.use(local).run(P, URL);
        class Fulfill extends Struct implements Task, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            run() throws Exception { forwarder.fulfill(a); }
        }
        _.enqueue.run(new Fulfill());
        return r;
    }
    
    public Object
    invoke( final String URL, final Object proxy,
           final Method method, final Object... arg) {
        return message(URL).invoke(URL, proxy, method, arg);
    }
    
    private Messenger
    message(final String URL) {
        if (!scheme.equals(URI.scheme("",URL))) {throw new RuntimeException();}
        
        final String authority = URI.authority(URL);
        final String location = Authority.location(authority);
        final String host = Location.host(location);
        final int port = Location.port(standardPort, location);
        final String peer = scheme + "://" + host.toLowerCase() +
                            (standardPort == port ? "" : ":" + port);
        final String peerKey = ".-" + URLEncoding.encode(peer);
        Messenger r = (Messenger)local.fetch(null, peerKey);
        if (null == r) {
            r = new Caller(local, peer);
            local.store(peerKey, r);
        }
        return r;
    }
}