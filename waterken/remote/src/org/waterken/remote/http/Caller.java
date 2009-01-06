// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Compose;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Invoke;
import org.ref_send.promise.eventual.Resolver;
import org.ref_send.type.Typedef;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.io.FileType;
import org.waterken.remote.Messenger;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.URI;
import org.web_send.Failure;

/**
 * Client-side of the HTTP web-amp protocol.
 */
/* package */ final class
Caller implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final String name;          // name of messaging session
    private final Pipeline msgs;        // queued HTTP requests
    private final Eventual _;           // eventual operator
    private final ClassLoader code;     // application code
    private final Exports exports;      // operations on web-keys
    
    private       String key = null;    // messaging session key
    
    Caller(final String name, final Pipeline msgs,
           final Eventual _, final ClassLoader code, final Exports exports) {
        this.name = name;
        this.msgs = msgs;
        this._ = _;
        this.code = code;
        this.exports = exports;
    }

    // org.waterken.remote.Messenger interface

    public <R> R
    when(final String href, final Class<?> R, final Do<Object,R> observer) {
        final R r_;
        final Do<Object,?> forwarder;
        if (void.class == R || Void.class == R) {
            r_ = null;
            forwarder = observer;
        } else {
            final Channel<R> x = _.defer();
            r_ = _.cast(R, x.promise);
            forwarder = new Compose<Object,R>(observer, x.resolver);
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
                    value = Eventual.promised(deserialize(href, response,
                                Compose.parameter(forwarder)));
                } catch (final Exception e) {
                    value = new Rejected<Object>(e);
                }
                _.got(value, forwarder, message);
                return null;
            }
            
            public Void
            reject(final Exception reason) {
                _.got(new Rejected<Object>(reason), forwarder, message);
                return null;
            }
        }
        final long mid = msgs.enqueue(new When());
        _.log.sent(name + "-" + mid);
        return r_;
    }
   
    public Object
    invoke(final String href, final Object proxy,
           final Method method, final Object... arg) {
        final ConstArray<?> argv= ConstArray.array(null==arg?new Object[0]:arg);
        return null != Exports.property(method)
            ? get(href, proxy.getClass(), method, argv)
        : post(href, proxy.getClass(), method, argv);
    }
    
    private Object
    get(final String href, final Class<?> type,
        final Method method, final ConstArray<?> argv) {
        final Channel<Object> r = _.defer();
        final Resolver<Object> resolver = r.resolver;
        class GET extends Operation implements Query {
            static private final long serialVersionUID = 1L;

            protected Message<Request>
            render() throws Exception {
                final String target=Exports.get(href, Exports.property(method));
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
                _.log.got(message, method);
                return receive(href, response, type, method, argv, resolver);
            }
            
            public Void
            reject(final Exception reason) {
                _.log.got(message, method);
                resolver.reject(reason);
                return null;
            }
        }
        final long mid = msgs.enqueue(new GET());
        _.log.sent(name + "-" + mid);
        return _.cast(Typedef.raw(Typedef.bound(method.getGenericReturnType(),
                                                type)), r.promise);
    }
    
    private Object
    post(final String URL, final Class<?> type,
         final Method method, final ConstArray<?> argv) {
        
        final Class<?> R =
            Typedef.raw(Typedef.bound(method.getGenericReturnType(), type));
        final Channel<Object> r = exports.request(R, URL, m, 0, 0);
        final Resolver<Object> resolver = null != r ? r.resolver : null;
        
        // schedule the message
        final String message = name + "-" + window + "-" + posts++;
        gets = 0;
        class POST extends Operation implements Update {
            static private final long serialVersionUID = 1L;
            
            protected Message<Request>
            render() throws Exception {
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
    receive(final String href, final Message<Response> response,
            final Class<?> type, final Method method,
            final ConstArray<?> argv, final Resolver<Object> resolver) {
        if ("404".equals(response.head.status)) {
            // re-dispatch this invocation on the resolved value of the web-key
            final Invoke<Object> invoke = new Invoke<Object>(method, argv);
            final Do<Object,?> forward = null == resolver
                ? invoke : new Compose<Object,Object>(invoke, resolver);
            Object x;
            try {
                x = exports.connect().run(href, null, Object.class);
            } catch (final Exception e) {
                x = new Rejected<Object>(e);
            }
            _.when(x, forward);
        } else if (null != resolver) {
            Volatile<Object> value;
            try {
                value = Eventual.promised(deserialize(href, response,
                    Typedef.bound(method.getGenericReturnType(), type)));
            } catch (final BadSyntax e) {
                /*
                 * strip out the parsing information to avoid leaking
                 * information to the application layer
                 */ 
                value = new Rejected<Object>((Exception)e.getCause());
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
            return new JSONDeserializer().run(href, connect,
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
