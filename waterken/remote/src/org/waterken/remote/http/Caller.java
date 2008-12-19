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
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.io.FileType;
import org.waterken.remote.Messenger;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.URI;
import org.waterken.vat.Root;
import org.waterken.vat.Vat;
import org.web_send.Failure;
import org.web_send.session.Session;

/**
 * Client-side of the HTTP web-amp protocol.
 */
final class
Caller implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final Pipeline msgs;
    private final Eventual _;
    private final ClassLoader code;
    private final Exports exports;
    
    private       long window = 0;  // last messaging window identifier
    private       int message = 0;  // number of POSTs in the last window
    private       Session session = null;   // messaging session configuration
    
    Caller(final Pipeline msgs, final Eventual _,
           final ClassLoader code, final Exports exports) {
        this.msgs = msgs;
        this._ = _;
        this.code = code;
        this.exports = exports;
    }

    // org.waterken.remote.Messenger interface

    /**
     * {@link Do} block parameter type
     */
    static private final TypeVariable<?> P = Typedef.var(Do.class, "P");

    public <R> R
    when(final String href, final Class<?> R, final Do<Object,R> observer) {
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
        class When extends Operation {
            static private final long serialVersionUID = 1L;

            protected Message<Request>
            render() throws Exception {
                final String target = Exports.get(href, ".");
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    )), null);
            }

            public Void
            fulfill(final Message<Response> response) {
                Volatile<Object> value;
                try {
                    final Type p = Typedef.value(P, observer.getClass());
                    value = Eventual.promised(deserialize(href, response, p));
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
    
    private Object
    get(final String URL, final Object proxy,
        final Method method, final ConstArray<?> argv) {
        final Channel<Object> r = _.defer();
        final Resolver<Object> resolver = r.resolver;
        class GET extends Operation implements Query {
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
                return receive(URL, response, proxy, method, argv, resolver);
            }
            
            public Void
            reject(final Exception reason) { return resolver.reject(reason); }
        }
        msgs.enqueue(new GET());
        return _.cast(Typedef.raw(Typedef.bound(method.getGenericReturnType(),
                                                proxy.getClass())), r.promise);
    }
    
    private Object
    post(final String URL, final Object proxy,
         final Method method, final ConstArray<?> argv) {
        
        // calculate the return pipeline web-key
        final String m = exports.mid();
        final Class<?> R = Typedef.raw(
            Typedef.bound(method.getGenericReturnType(), proxy.getClass()));
        final Channel<Object> r = exports.request(R, URL, m, 0, 0);
        final Resolver<Object> resolver = null != r ? r.resolver : null;
        
        // schedule the message
        class POST extends Operation implements Update {
            static private final long serialVersionUID = 1L;
            
            Request
            send() throws Exception {
                return serialize(URI.resolve(URL, "?p=" + method.getName() +
                        "&s=" + Exports.key(URL) + "&m=" + m), argv);
            }

            public Void
            fulfill(final Response response) {
                exports.response(m, 0, 0);
                return receive(URL, response, proxy, method, argv, resolver);
            }
            
            public Void
            reject(final Exception reason) {
                return null != resolver ? resolver.reject(reason) : null;
            }
        }
        msgs.enqueue(new POST());
        return null != r ? _.cast(R, r.promise) : null;
    }
    
    private Void
    receive(final String target, final Response response,
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
            Object src;
            try {
                src = exports.connect().run(
                        Exports.asPromise(target), null, Object.class);
            } catch (final Exception e) {
                src = new Rejected<Object>(e);
            }
            _.when(src, new Retry());
        } else if (null != resolver) {
            Volatile<Object> value;
            try {
                final Type R = Typedef.bound(
                    method.getGenericReturnType(), proxy.getClass());
                value = Eventual.promised(deserialize(target, response, R));
            } catch (final Exception e) {
                value = new Rejected<Object>(e);
            }
            resolver.resolve(value);
        }
        return null;
    }
    
    private Message<Request>
    serialize(final String target, final ConstArray<?> argv) throws Exception {
        final String authority = URI.authority(target);
        final String location = Authority.location(authority);
        if (argv.length() == 1 && argv.get(0) instanceof ByteArray) {
            final ByteArray body = (ByteArray)argv.get(0);
            return new Message<Request>(new Request(
                "HTTP/1.1", "POST", URI.request(target),
                PowerlessArray.array(
                    new Header("Host", location),
                    new Header("Content-Type", FileType.unknown.name),
                    new Header("Content-Length", "" + body.length())
                )), body);        
        }
        final String base = URI.resolve(target, "."); 
        final ByteArray body =
            new JSONSerializer().run(exports.render(base), argv);  
        return new Message<Request>(new Request(
            "HTTP/1.1", "POST", URI.request(target),
            PowerlessArray.array(
                new Header("Host", location),
                new Header("Content-Type", FileType.json.name),
                new Header("Content-Length", "" + body.length())
            )), body);        
    }
    
    private Object
    deserialize(final String href,
                final Message<Response> m, final Type R) throws Exception {
        final Importer connect = exports.connect();
        if ("200".equals(m.head.status) || "201".equals(m.head.status) ||
            "202".equals(m.head.status) || "203".equals(m.head.status)) {
            if (Header.equivalent(FileType.unknown.name,
                                  m.head.getContentType())) {
                return ConstArray.array(m.body);
            }
            return new JSONDeserializer().run(URI.resolve(href, "."), connect,
                ConstArray.array(R), code, m.body.asInputStream()).get(0);
        } 
        if ("204".equals(m.head.status) ||
            "205".equals(m.head.status)) { return null; }
        if ("303".equals(m.head.status)) {
            for (final Header h : m.head.headers) {
                if (Header.equivalent("Location", h.name)) {
                    return connect.run(h.value, null, R);
                }
            }
            return null;    // request accepted, but no response provided
        } 
        throw new Failure(m.head.status, m.head.phrase);
    }
}
