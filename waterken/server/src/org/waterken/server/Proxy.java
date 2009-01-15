// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.cache.Cache;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Server;
import org.waterken.net.Locator;
import org.waterken.net.http.ClientSide;
import org.waterken.thread.Concurrent;
import org.waterken.thread.Sleep;
import org.waterken.uri.URI;

/**
 * The HTTP gateway for this JVM.
 */
public final class
Proxy extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    static private final ThreadGroup threads = new ThreadGroup("HTTP");
    static private final Cache<String,Server> connections = Cache.make();
    
    static private synchronized Server
    connect(final String peer, final Locator transport) {
        Server r = connections.fetch(null, peer);
        if (null == r) {
            final Receiver<ClientSide.Outbound> sender =
                Concurrent.make(threads, "=>" + peer);
            final Receiver<ClientSide.Inbound> receiver =
                Concurrent.make(threads, "<=" + peer);
            r = ClientSide.make(peer, transport, new Sleep(), sender, receiver);
            connections.put(peer, r);
        }
        return r;
    }

    static private final HashMap<String,Locator> protocols =
        new HashMap<String,Locator>();
    static private       Credentials credentials = null;
    
    static public Credentials
    init() {
        if (!protocols.isEmpty()) { throw new Error(); }
        Proxy.protocols.put("http", Loopback.client(80));
        if (Settings.keys.isFile()) {
            credentials = SSL.keystore("TLS", Settings.keys, "nopass");
            Proxy.protocols.put("https", SSL.client(443, credentials));
        }
        return credentials;
    }
    
    public void
    serve(final String resource, final Request head, final InputStream body,
                                 final Client client) throws Exception{
        final String scheme = URI.scheme(resource);
        final Locator transport = protocols.get(scheme);
        final String authority= transport.canonicalize(URI.authority(resource));
        final String peer = scheme + "://" + authority + "/";
        connect(peer, transport).serve(resource, head, body, client);
    }
}
