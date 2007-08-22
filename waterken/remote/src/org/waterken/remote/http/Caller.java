// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.security.SecureRandom;

import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.list.List;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Resolver;
import org.ref_send.type.Typedef;
import org.waterken.http.Failure;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.id.Importer;
import org.waterken.id.base.Base;
import org.waterken.id.exports.Exports;
import org.waterken.io.MediaType;
import org.waterken.io.buffer.Buffer;
import org.waterken.io.limited.Limited;
import org.waterken.io.limited.TooMuchData;
import org.waterken.model.Effect;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Service;
import org.waterken.model.Transaction;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.syntax.Serializer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.syntax.json.Java;
import org.waterken.uri.Authority;
import org.waterken.uri.Base32;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * Manages a pending request queue for a specific peer.
 */
final class
Caller implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final Root local;
    private final String peer;
    
    private final Eventual _;
    private final List<Message> pending;
    private       int serialMID;
    private       int halts;        // number of pending pipeline flushes
    private       int queries;      // number of queries after the last flush
    /*
     * Message sending is halted when an Update follows a Query. Sending resumes
     * once the response to the preceeding Query has been received.
     */
    
    Caller(final Root local, final String peer) {
        this.local = local;
        this.peer = peer;
        
        _ = (Eventual)local.fetch(null, Remoting._);
        pending = List.list();
        serialMID = 0;
        halts = 0;
        queries = 0;
    }

    /**
     * {@link Do} block parameter type
     */
    static private final TypeVariable DoP = Typedef.name(Do.class, "P");

    @SuppressWarnings("unchecked") public <P,R> R
    when(final String URL, final Class<?> R, final Do<P,R> observer) {
        final R r_;
        final Resolver<R> resolver;
        if (void.class == R || Void.class == R) {
            r_ = null;
            resolver = null;
        } else {
            final Channel<R> x = _.defer();
            r_ = R.isAssignableFrom(Promise.class)
                    ? (R)x.promise : _.cast(R, x.promise);
            resolver = x.resolver;
        }
        final String target = URI.resolve(URL, "?o=" + Exports.key(URL));
        class When extends Message {
            static private final long serialVersionUID = 1L;

            When() { super(serialMID++); }

            Request
            send() throws Exception {
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Request("HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    ), null);
            }

            public Void
            fulfill(final Response response) {
                final Type P = Typedef.value(DoP, observer.getClass());
                final Volatile<P> value = deserialize(P, target, response);
                final R r = _.when(value, observer);
                if (null != resolver) {resolver.resolve(Eventual.promised(r));}
                return null;
            }
            
            public Void
            reject(final Exception reason) throws Exception {
                final R r = _.when(new Rejected<P>(reason), observer);
                if (null != resolver) {resolver.resolve(Eventual.promised(r));}
                return null;
            }
        }
        enqueue(new When());
        return r_;
    }
   
    public Object
    invoke(final String URL, final Object proxy,
           final Method method, final Object... arg) {
        return null != Java.property(method)
            ? get(URL, proxy, method)
        : post(URL, proxy, method, arg);
    }
    
    @SuppressWarnings("unchecked") private <R> R
    get(final String URL, final Object proxy, final Method method) {
        final Channel<R> r = _.defer();
        final Resolver<R> resolver = r.resolver;
        final String target = URI.resolve(URL,
            "?s=" + Exports.key(URL) + "&p=" + Java.property(method));
        class GET extends Query {
            static private final long serialVersionUID = 1L;

            GET() { super(serialMID++); }

            Request
            send() throws Exception {
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Request("HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    ), null);
            }

            public Void
            fulfill(final Response response) throws Exception {
                done();
                if ("404".equals(response.status) && null != Exports.src(URL)) {
                    class Retry extends Do<Object,Void> implements Serializable{
                        static private final long serialVersionUID = 1L;

                        public Void
                        fulfill(final Object object) {
                            final R value;
                            try {
                                // AUDIT: call to untrusted application code
                                value = (R)Reflection.invoke(method,
                                    object instanceof Volatile
                                        ? _.cast(method.getDeclaringClass(),
                                                 (Volatile)object)
                                    : object);
                            } catch (final Exception reason) {
                                return resolver.reject(reason);
                            }
                            return resolver.resolve(Eventual.promised(value));
                        }
                        
                        public Void
                        reject(final Exception reason) {
                            return resolver.reject(reason);
                        }
                    }
                    return _.when(proxy, new Retry());
                }
                final Type R = Typedef.bound(method.getGenericReturnType(),
                                             proxy.getClass());
                final Volatile<R> value = deserialize(R, target, response);
                return resolver.resolve(value);
            }
            
            public Void
            reject(final Exception reason) throws Exception {
                done();
                return resolver.reject(reason);
            }
            
            private void
            done() {
                if (0 == halts) {
                    --queries;
                } else {
                    for (final Message m : pending) {
                        if (m instanceof Query) { break; }
                        if (m instanceof Update) {
                            --halts;
                            restart(true, m.id);
                            break;
                        }
                    }
                }
            }
        }
        enqueue(new GET());
        ++queries;
        final Class<?> R = Typedef.raw(
            Typedef.bound(method.getGenericReturnType(), proxy.getClass()));
        return R.isAssignableFrom(Promise.class)
            ? (R)r.promise
        : _.cast(R, r.promise);
    }
    
    @SuppressWarnings("unchecked") private <R> R
    post(final String URL, final Object proxy,
         final Method method, final Object... arg) {
        
        // generate a message key
        final byte[] secret = new byte[16];
        final SecureRandom prng = (SecureRandom)local.fetch(null, Root.prng);
        prng.nextBytes(secret);
        final String m = Base32.encode(secret);

        // calculate the return pipeline web-key
        final Class<?> R = Typedef.raw(
            Typedef.bound(method.getGenericReturnType(), proxy.getClass()));
        final R r_;
        final Resolver<R> resolver;
        if (void.class == R || Void.class == R) {
            r_ = null;
            resolver = null;
        } else {
            final String pipe = Exports.pipeline(m);
            final Channel<R> x = _.defer();
            local.store(pipe, x.promise);
            r_ = (R)Remote.use(local).run(R, Exports.href(URI.resolve(URL, "."),
                     (String)local.fetch(null, Remoting.here), pipe));
            resolver = x.resolver;
        }
        
        // schedule the message
        final String target = URI.resolve(URL,
            "?s=" + Exports.key(URL) + "&p=" + method.getName() + "&m=" + m);
        final ConstArray<?> argv =
            ConstArray.array(null == arg ? new Object[0] : arg);
        class POST extends Update {
            static private final long serialVersionUID = 1L;

            POST() { super(serialMID++); }

            Request
            send() throws Exception {
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                final Buffer content = Buffer.copy(new JSONSerializer().
                    run(Serializer.render, Java.bind(ID.bind(Base.relative(
                        URI.resolve(target, "."), Base.absolute(
                            (String)local.fetch(null, Remoting.here),
                            Remote.bind(local, Exports.bind(local)))))), argv));
                return new Request("HTTP/1.1", "POST", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location),
                        new Header("Content-Type", MediaType.json.name),
                        new Header("Content-Length", "" + content.length)
                    ), content);
            }

            public Void
            fulfill(final Response response) throws Exception {
                if ("404".equals(response.status) && null != Exports.src(URL)) {
                    class Retry extends Do<Object,Void> implements Serializable{
                        static private final long serialVersionUID = 1L;

                        public Void
                        fulfill(final Object object) {
                            final R value;
                            try {
                                // AUDIT: call to untrusted application code
                                value = (R)Reflection.invoke(method,
                                    object instanceof Volatile
                                        ? _.cast(method.getDeclaringClass(),
                                                 (Volatile)object)
                                    : object,
                                    argv.toArray(new Object[argv.length()]));
                            } catch (final Exception reason) {
                                return resolver.reject(reason);
                            }
                            return resolver.resolve(Eventual.promised(value));
                        }
                        
                        public Void
                        reject(final Exception reason) {
                            return resolver.reject(reason);
                        }
                    }
                    return _.when(proxy, new Retry());
                }
                if (null != resolver) {
                    final Type R = Typedef.bound(method.getGenericReturnType(),
                                                 proxy.getClass());
                    final Volatile<R> value = deserialize(R, target, response);
                    resolver.resolve(value);
                }
                return null;
            }
            
            public Void
            reject(final Exception reason) throws Exception {
                return resolver.reject(reason);
            }
        }
        if (0 != queries) {
            ++halts;
            queries = 0;
        }
        enqueue(new POST());
        return r_;
    }
    
    @SuppressWarnings("unchecked") private void
    enqueue(final Message message) {
        if (pending.isEmpty()) {
            ((Outbound)local.fetch(null, AMP.outbound)).add(peer, this);
        }
        pending.append(message);
        if (0 != halts) { return; }

        // Create a transaction effect that will schedule a new extend
        // transaction that actually puts the message on the wire.
        final Loop<Effect> effect = (Loop)local.fetch(null, Root.effect);
        final Model model = (Model)local.fetch(null, Root.model);
        final String peer = this.peer;
        final int mid = message.id;
        effect.run(new Effect() {
           public void
           run() throws Exception {
               model.service.run(new Service() {
                   public void
                   run() throws Exception {
                       model.enter(Model.extend, new Transaction<Void>() {
                           public Void
                           run(final Root local) throws Exception {
                               final Caller caller = ((Outbound)local.
                                       fetch(null, AMP.outbound)).find(peer);
                               for (final Message m : caller.pending) {
                                   if (mid == m.id) {
                                       caller.send(m);
                                       break;
                                   }
                               }
                               return null;
                           }
                       });
                   }
               });
           }
        });
    }
    
    @SuppressWarnings("unchecked") void
    restart(final boolean skipTo, final int mid) {
        // Create a transaction effect that will schedule a new extend
        // transaction that actually puts the messages on the wire.
        final int max = pending.getSize();
        final Loop<Effect> effect = (Loop)local.fetch(null, Root.effect);
        final Model model = (Model)local.fetch(null, Root.model);
        final String peer = this.peer;
        effect.run(new Effect() {
           public void
           run() throws Exception {
               model.service.run(new Service() {
                   public void
                   run() throws Exception {
                       model.enter(Model.extend, new Transaction<Void>() {
                           public Void
                           run(final Root local) throws Exception {
                               final Caller caller = ((Outbound)local.
                                       fetch(null, AMP.outbound)).find(peer);
                               boolean found = !skipTo;
                               boolean q = false;
                               int n = max;
                               for (final Message m : caller.pending) {
                                   if (0 == n--) { break; }
                                   if (!found) {
                                       if (mid == m.id) {
                                           found = true;
                                       } else {
                                           continue;
                                       }
                                   }
                                   if (q && m instanceof Update) { break; }
                                   if (m instanceof Query) { q = true; }
                                   caller.send(m);
                               }
                               return null;
                           }
                       });
                   }
               });
           }
        });
    }
    
    @SuppressWarnings("unchecked") private void
    send(final Message message) {
        Promise<Request> rendered;
        try {
            rendered = Fulfilled.ref(message.send());
        } catch (final Exception reason) {
            rendered = new Rejected<Request>(reason);
        }
        final Promise<Request> request = rendered;
        final Server client = (Server)local.fetch(null, Remoting.client);
        final Loop<Effect> effect = (Loop)local.fetch(null, Root.effect);
        final Model model = (Model)local.fetch(null, Root.model);
        final String peer = this.peer;
        final int mid = message.id;
        effect.run(new Effect() {
           public void
           run() throws Exception {
               client.serve(peer, request, new Receive(model, peer, mid));
           }
        });
    }
    
    static private final class
    Receive extends Do<Response,Void> {
        
        private final Model model;
        private final String peer;
        private final int mid;
        
        Receive(final Model model, final String peer, final int mid) {
            this.model = model;
            this.peer = peer;
            this.mid = mid;
        }

        public Void
        fulfill(Response r) throws Exception {
            if (null != r.body) {
                try {
                    r = new Response(r.version, r.status, r.phrase, r.header,
                        Buffer.copy(Limited.limit(AMP.maxContentSize, r.body)));
                } catch (final TooMuchData e) {
                    return reject(e);
                }
            }
            return resolve(Fulfilled.ref(r));
        }
        
        public Void
        reject(final Exception reason) throws Exception {
            return resolve(new Rejected<Response>(reason));
        }
        
        private Void
        resolve(final Promise<Response> response) throws Exception {
            return model.enter(Model.change, new Transaction<Void>() {
                public Void
                run(final Root local) throws Exception {
                    final Outbound outbound =
                        (Outbound)local.fetch(null, AMP.outbound);
                    final Caller caller = outbound.find(peer);
                    final Message m = caller.pending.getFront();
                    if (mid != m.id) { throw new Exception(); }
                    caller.pending.pop();
                    if (caller.pending.isEmpty()) {
                        outbound.remove(peer);
                    }
                    Response value;
                    try {
                        value = response.cast();
                    } catch (final Exception reason) {
                        return m.reject(reason);
                    }
                    return m.fulfill(value);
                }
            });
        }
    }
    
    @SuppressWarnings("unchecked") private <R> Volatile<R>
    deserialize(final Type R, final String target, final Response response) {
        final String base = URI.resolve(target, ".");
        final ClassLoader code = (ClassLoader)local.fetch(null, Root.code);
        final String here = (String)local.fetch(null, Remoting.here);
        final Importer connect = Exports.use(here, Exports.make(local),
            Java.use(base, code, ID.use(base, Remote.use(local)))); 
        if ("200".equals(response.status) || "201".equals(response.status) ||
            "202".equals(response.status) || "203".equals(response.status)) {
            if (!MediaType.json.name.equals(response.getContentType())) {
                return new Rejected<R>(Failure.unsupported);
            }
            try {
                return Eventual.promised((R)(new JSONDeserializer().
                    run(base, connect, code,
                        ((Buffer)response.body).open(),
                        PowerlessArray.array(R)).get(0)));
            } catch (final Exception e) {
                return new Rejected<R>(e);
            }
        } 
        if ("204".equals(response.status) ||
            "205".equals(response.status)) { return null; }
        if ("303".equals(response.status)) {
            for (final Header h : response.header) {
                if ("Location".equalsIgnoreCase(h.name)) {
                    return Eventual.promised((R)connect.run(Typedef.raw(R),
                                                            h.value));
                }
            }
            return null;    // request accepted, but no response provided
        } 
        return new Rejected<R>(new Failure(response.status, response.phrase));
    }
}
