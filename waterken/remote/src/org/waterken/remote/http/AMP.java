// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.id.exports.Exports;
import org.waterken.io.buffer.Buffer;
import org.waterken.io.limited.Limited;
import org.waterken.model.Creator;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Transaction;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.uri.Header;
import org.waterken.uri.URI;
import org.web_send.graph.Framework;
import org.web_send.graph.Host;

/**
 * HTTP web-AMP implementation
 */
public final class
AMP extends Struct implements Remoting, Powerless, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * maximum number of bytes in a message entity
     */
    static /* package */ final long maxContentSize = 64 * 1024;
    
    // org.waterken.remote.Remoting interface

    public Server
    remote(final Server bootstrap, final String scheme, final Model model) {
        return new Server() {
            public void
            serve(final String resource,
                  final Volatile<Request> requestor,
                  final Do<Response,?> respond) throws Exception {
                String httpRequestVersion;
                Promise<Request> buffered;
                boolean extend;
                try {
                    Request q = requestor.cast();
                    if (null != q.body) {
                        q = new Request(q.version, q.method, q.URI, q.header,
                            Buffer.copy(Limited.limit(maxContentSize, q.body)));
                    }
                    httpRequestVersion = q.version;
                    buffered = ref(q);
                    extend = "GET".equals(q.method) ||
                             "HEAD".equals(q.method) ||
                             "OPTIONS".equals(q.method) ||
                             "TRACE".equals(q.method);
                } catch (final Exception e) {
                    httpRequestVersion = "HTTP/1.1";
                    buffered = new Rejected<Request>(e);
                    extend = true;
                }
                final String httpVersion = httpRequestVersion;
                final Promise<Request> requested = buffered;
                model.enter(extend, new Transaction<Void>() {
                    public Void
                    run(final Root local) throws Exception {
                        new Callee(bootstrap, local).
                            serve(resource, requested, new Do<Response,Void>() {

                                public Void
                                fulfill(Response r) throws Exception {
                                    if ("HTTP/1.0".equals(httpVersion) &&
                                            null != r.body) {
                                        // enable a persistent connection by
                                        // creating a self-delimited response
                                        final Buffer body = Buffer.copy(Limited.
                                            limit(maxContentSize, r.body));
                                        r = new Response(r.version, r.status,
                                            r.phrase, r.header.with(
                                                new Header("Content-Length",
                                                           "" + body.length)),
                                            body);
                                    }
                                    respond.fulfill(r);
                                    return null;
                                }
                                
                                public Void
                                reject(final Exception e) throws Exception {
                                    respond.reject(e);
                                    return null;
                                }
                            });
                        return null;
                    }
                });
            }
        };
    }

    // org.waterken.remote.http.AMP interface
    
    static public Host
    host(final Root mother) {
        class HostX extends Struct implements Host, Serializable {
            static private final long serialVersionUID = 1L;

            @SuppressWarnings("unchecked") public <T> Promise<T>
            share(final String label, final String typename) {
                final String base = (String)mother.fetch(null, here);
                final Server client= (Server)mother.fetch(null,Remoting.client);
                final Creator create = (Creator)mother.fetch(null, Root.create);
                final String URL = create.run(label, new Transaction<String>() {
                    public String
                    run(final Root local) throws Exception {
                        final String here = base+URLEncoding.encode(label)+"/";
                        local.store(Remoting.here, here);
                        if (null != client) {
                            local.store(Remoting.client, client);
                            local.store(Root.wake, new Wake());
                            local.store(outbound, new Outbound());
                        }
                        final Token deferred = new Token();
                        local.store(Remoting.deferred, deferred);
                        final Eventual _ = new Eventual(deferred,
                            (Loop)local.fetch(null, Root.enqueue));
                        local.store(Remoting._, _);
                        Exports.initialize(local);
                        final Framework framework = new Framework(
                            _,
                            Exports.make(local),
                            (Runnable)local.fetch(null, Root.destruct),
                            host(local)
                        );
                        final ClassLoader code =
                            (ClassLoader)local.fetch(null, Root.code);
                        final Class<?> factory = code.loadClass(typename);
                        Object app;
                        try {
                            final Method build = Reflection.method(
                                    factory, "build", Framework.class);
                            app = Reflection.invoke(build, null, framework);
                        } catch (final NoSuchMethodException e) {
                            final Constructor make =
                                Reflection.constructor(factory, Eventual.class);
                            app = Reflection.construct(make, _);
                        }
                        return URI.resolve(here, Exports.bind(local).run(app));
                    }
                }, (String)mother.fetch(null, Root.project));
                return (Promise<T>)Remote.use(mother).run(Object.class, URL);
            }
        }
        return new HostX();
    }

    static private final class
    Wake extends Struct implements Transaction<Void>, Serializable {
        static private final long serialVersionUID = 1L;

        public Void
        run(final Root local) throws Exception {
            final Outbound outbound = (Outbound)local.fetch(null, AMP.outbound);
            for (final Outbound.Entry x : outbound.getPending()) {
                x.msgs.resend();
            }
            return null;
        }
    }
    
    static /* package */ final String outbound = ".outbound";
}
