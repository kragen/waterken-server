// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.InputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.cache.Cache;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Server;
import org.waterken.http.TokenList;
import org.waterken.net.Locator;
import org.waterken.net.http.ClientSide;
import org.waterken.thread.Concurrent;
import org.waterken.thread.Sleep;
import org.waterken.uri.Header;
import org.waterken.uri.Location;

/**
 * The HTTPS gateway for this JVM.
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

    static private   final Locator http = Loopback.client(80);
    static protected final Credentials credentials = Settings.keys.isFile()
        ? SSL.keystore("TLS", Settings.keys, "nopass") : null;
    static private   final Locator https =
        null != credentials ? SSL.client(443, credentials) : null;
    
    public void
    serve(final Request head,
          final InputStream body, final Client client) throws Exception {
        // TODO: implement boot comm
        final String host = TokenList.find("localhost", "Host", head.headers);
        final Locator transport =
            Header.equivalent("localhost", Location.hostname(host))?http:https;
        connect(transport.canonicalize(host),transport).serve(head,body,client);
    }
}
