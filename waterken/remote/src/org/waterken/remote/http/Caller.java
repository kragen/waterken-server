// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Resolver;
import org.ref_send.type.Typedef;
import org.waterken.http.MediaType;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.TokenList;
import org.waterken.io.snapshot.Snapshot;
import org.waterken.remote.Exports;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remoting;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.URI;
import org.waterken.vat.Root;
import org.web_send.Entity;
import org.web_send.Failure;

/**
 * Client-side of the HTTP web-amp protocol.
 */
final class
Caller extends Struct implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final Pipeline msgs;
    private final Eventual _;
    private final ClassLoader code;
    private final Exports exports;
    
    Caller(final Pipeline msgs, final Root local) {
        this.msgs = msgs;
        _ = local.fetch(null, Remoting._);
        code = local.fetch(null, Root.code);
        exports = new Exports(local);
    }

    // org.waterken.remote.Messenger interface

    /**
     * {@link Do} block parameter type
     */
    static private final TypeVariable<?> DoP = Typedef.name(Do.class, "P");

    public <R> R
    when(final String URL, final Class<?> R, final Do<Object,R> observer) {
        final R r_;
        final Resolver<R> resolver;
        if (void.class == R || Void.class == R) {
            r_ = null;
            resolver = null;
        } else {
            final Channel<R> x = _.defer();
            r_ = _.cast(R, x.promise);
            resolver = x.resolver;
        }
        class When extends Message {
            static private final long serialVersionUID = 1L;

            Request
            send() throws Exception {
                final String target = URI.resolve(URL, "?s="+Exports.key(URL));
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Request("HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    ), null);
            }

            public Void
            fulfill(final Response response) {
                Volatile<Object> value;
                try {
                    final Type P = Typedef.value(DoP, observer.getClass());
                    value = Eventual.promised(deserialize(P, URL, response));
                } catch (final Exception e) {
                    value = new Rejected<Object>(e);
                }
                final R r = when(value, observer);
                if (null != resolver) { resolver.run(r); }
                return null;
            }
            
            public Void
            reject(final Exception reason) {
                final R r = when(new Rejected<Object>(reason), observer);
                if (null != resolver) { resolver.run(r); }
                return null;
            }
            
            /**
             * A trick to resolve a dispatch ambiguity in javac.
             */
            private <P,O> O
            when(final Volatile<P> value, final Do<P,O> observer) {
            	return _.when(value, observer);
            }
        }
        msgs.enqueue(new When());
        return r_;
    }
   
    public Object
    invoke(final String URL, final Object proxy,
           final Method method, final Object... arg) {
        final ConstArray<?> argv= ConstArray.array(null==arg?new Object[0]:arg);
        return null != Exports.property(method)
            ? get(URL, proxy, method, argv)
        : post(URL, proxy, method, argv);
    }
    
    private @Deprecated Void
    put(final String URL, final Object proxy,
        final Method method, final ConstArray<?> argv) {
        class PUT extends Message implements Update, Query {
            static private final long serialVersionUID = 1L;

            Request
            send() throws Exception {
                return serialize(URI.resolve(URL, "?p=run&s="+Exports.key(URL)),
                                 argv);
            }

            public Void
            fulfill(final Response response) {
                return receive(response, URL, proxy, method, argv, null);
            }
        }
        msgs.enqueue(new PUT());
        return null;
    }
    
    private Object
    get(final String URL, final Object proxy,
        final Method method, final ConstArray<?> argv) {
        final Channel<Object> r = _.defer();
        final Resolver<Object> resolver = r.resolver;
        class GET extends Message implements Query {
            static private final long serialVersionUID = 1L;

            Request
            send() throws Exception {
                final String target = URI.resolve(URL,
                    "?p=" + Exports.property(method) + "&s="+ Exports.key(URL));
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Request("HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    ), null);
            }

            public Void
            fulfill(final Response response) {
                return receive(response, URL, proxy, method, argv, resolver);
            }
            
            public Void
            reject(final Exception reason) { return resolver.reject(reason); }
        }
        msgs.enqueue(new GET());
        final Class<?> R = Typedef.raw(
            Typedef.bound(method.getGenericReturnType(), proxy.getClass()));
        return void.class == R || Void.class == R ? null : _.cast(R, r.promise);
    }
    
    private Object
    post(final String URL, final Object proxy,
         final Method method, final ConstArray<?> argv) {
        
        // calculate the return pipeline web-key
        final String m = exports.mid();
        final Class<?> R = Typedef.raw(
            Typedef.bound(method.getGenericReturnType(), proxy.getClass()));
        final Object r_;
        final Resolver<Object> resolver;
        if (void.class == R || Void.class == R) {
            r_ = null;
            resolver = null;
        } else {
            final Channel<Object> x = _.defer();
            r_ = exports.far(URI.resolve(URL, "."), m, R, x.promise);
            resolver = x.resolver;
        }
        
        // schedule the message
        exports.sent(m);
        class POST extends Message implements Update {
            static private final long serialVersionUID = 1L;
            
            Request
            send() throws Exception {
                return serialize(URI.resolve(URL, "?p=" + method.getName() +
                        "&s=" + Exports.key(URL) + "&m=" + m), argv);
            }

            public Void
            fulfill(final Response response) {
                exports.received(m);
                return receive(response, URL, proxy, method, argv, resolver);
            }
            
            public Void
            reject(final Exception reason) {
                return null != resolver ? resolver.reject(reason) : null;
            }
        }
        msgs.enqueue(new POST());
        return r_;
    }
    
    private Void
    receive(final Response response, final String URL,
            final Object proxy, final Method method,
            final ConstArray<?> argv, final Resolver<Object> resolver) {
        if ("404".equals(response.status)) {
            class Retry extends Do<Object,Void> implements Serializable {
                static private final long serialVersionUID = 1L;

                public Void
                fulfill(final Object object) throws Exception {
                    final Object r = Reflection.invoke(method,
                        _.cast(method.getDeclaringClass(),
                               Eventual.promised(object)),
                        argv.toArray(new Object[argv.length()]));
                    if (null != resolver) { resolver.run(r); }
                    return null;
                }
                
                public Void
                reject(final Exception reason) {
                    if (null != resolver) { resolver.reject(reason); }
                    return null;
                }
            }
            final String target = Exports.isPromise(URL)
                ? URL
            : URI.resolve(URL, "?src=#" + URI.fragment("", URL));
            _.when(exports.connect().run(Object.class,target,null),new Retry());
        } else if (null != resolver) {
            Volatile<Object> value;
            try {
                final Type R = Typedef.bound(
                    method.getGenericReturnType(), proxy.getClass());
                value = Eventual.promised(deserialize(R, URL, response));
            } catch (final Exception e) {
                value = new Rejected<Object>(e);
            }
            resolver.resolve(value);
        }
        return null;
    }
    
    private Request
    serialize(final String target, final ConstArray<?> argv) throws Exception {
        final String authority = URI.authority(target);
        final String location = Authority.location(authority);
        if (argv.length() == 1 && argv.get(0) instanceof Entity) {
            final Entity x = (Entity)argv.get(0);
            return new Request("HTTP/1.1", "POST", URI.request(target),
                PowerlessArray.array(
                    new Header("Host", location),
                    new Header("Content-Type", x.type),
                    new Header("Content-Length", "" + x.content.length())
                ), new Snapshot(x.content));        
        }
        final String base = URI.resolve(target, "."); 
        final ByteArray.BuilderOutputStream out =
            ByteArray.builder(1024).asOutputStream();
        new JSONSerializer().run(exports.send(base), argv, out);
        final Snapshot body = new Snapshot(out.snapshot());          
        return new Request("HTTP/1.1", "POST", URI.request(target),
            PowerlessArray.array(
                new Header("Host", location),
                new Header("Content-Type", AMP.mime.toString()),
                new Header("Content-Length", "" + body.content.length())
            ), body);        
    }
    
    private Object
    deserialize(final Type R, final String target,
                final Response response) throws Exception {
        final Importer connect = exports.connect();
        if ("200".equals(response.status) || "201".equals(response.status) ||
            "202".equals(response.status) || "203".equals(response.status)) {
            final ByteArray content = ((Snapshot)response.body).content;
            final String contentType = response.getContentType();
            final MediaType mediaType = 
            	null != contentType ? MediaType.decode(contentType) : AMP.mime;
            if (!AMP.mime.contains(mediaType)) {
                return new Entity(contentType, content);
            }
            if (!TokenList.equivalent("UTF-8",
                                      mediaType.get("charset", "UTF-8"))) {
                throw new Exception("charset MUST be UTF-8");
            }
            return new JSONDeserializer().run(URI.resolve(target, "."), connect,
                code, content.asInputStream(), PowerlessArray.array(R)).get(0);
        } 
        if ("204".equals(response.status) ||
            "205".equals(response.status)) { return null; }
        if ("303".equals(response.status)) {
            for (final Header h : response.header) {
                if ("Location".equalsIgnoreCase(h.name)) {
                    return connect.run(R, h.value, null);
                }
            }
            return null;    // request accepted, but no response provided
        } 
        throw new Failure(response.status, response.phrase);
    }
}
