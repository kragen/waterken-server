// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.Serializable;
import java.util.HashMap;

import org.joe_e.Struct;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Loop;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.net.Locator;
import org.waterken.net.Execution;
import org.waterken.net.http.Client;
import org.waterken.thread.Concurrent;
import org.waterken.uri.URI;

/**
 * 
 */
final class
Proxy extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    static private final ThreadGroup threads = new ThreadGroup("HTTP");
    static /* package */ final Execution thread = new Execution() {
        public void
        sleep(final long ms) throws InterruptedException { Thread.sleep(ms); }
        
        public void
        yield() { Thread.yield(); }
    };
    static private final Cache<Server> connections = new Cache<Server>();
    
    static private synchronized Server
    connect(final String peer, final Locator transport) {
        Server r = connections.fetch(null, peer);
        if (null == r) {
            final Loop<Client.Outbound> sender =
                Concurrent.loop(threads, "=>" + peer);
            final Loop<Client.Inbound> receiver =
                Concurrent.loop(threads, "<=" + peer);
            r = Client.make(peer, transport, thread, sender, receiver);
            connections.put(peer, r);
        }
        return r;
    }

    static /* package */ final HashMap<String,Locator> protocols =
        new HashMap<String,Locator>();
    
    public void
    serve(final String resource,
          final Volatile<Request> request,
          final Do<Response,?> respond) throws Exception {
        final String scheme = URI.scheme(null, resource).toLowerCase();
        final Locator transport = protocols.get(scheme);
        final String authority= transport.canonicalize(URI.authority(resource));
        final String peer = scheme + "://" + authority + "/";
        connect(peer, transport).serve(resource, request, respond);
    }
}
