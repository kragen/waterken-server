// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.promise.Channel;
import org.ref_send.promise.Deferred;
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Failure;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Resolver;
import org.ref_send.promise.Volatile;
import org.ref_send.type.Typedef;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.io.FileType;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remote;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * Client-side of the HTTP web-amp protocol.
 */
/* package */ final class
Caller extends Struct implements Messenger, Serializable {
    static private final long serialVersionUID = 1L;

    private final Eventual _;
    private final String here;
    private final ClassLoader codebase;
    private final Importer connect;
    private final Exporter export;
    private final Pipeline msgs;        // queued HTTP requests
    
    protected
    Caller(final Eventual _, final String here, final ClassLoader codebase,
           final Importer connect, final Exporter export, final Pipeline msgs) {
        this._ = _;
        this.here = here;
        this.codebase = codebase;
        this.connect = connect;
        this.export = export;
        this.msgs = msgs;
    }

    // org.waterken.remote.Messenger interface

    public void
    when(final String href, final Remote proxy, final Do<Object,?> observer) {
        final String base = URI.resolve(here, href);
        class When extends Operation implements Serializable {
            static private final long serialVersionUID = 1L;

            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String target = HTTP.get(base, ".");
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    )), null);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                _.log.got(request, null, null);
                _.when(receive(base, response, Deferred.parameter(observer)),
                       observer);
                // TODO: implement polling on a 404 response?
            }
            
            public void
            reject(final String request, final Exception reason) {
                _.log.got(request, null, null);
                _.when(new Rejected<Object>(reason), observer);
            }
        }
        _.log.sent(msgs.enqueue(new When()));
    }
   
    public Object
    invoke(final String href, final Object proxy,
           final Method method, final Object... arg) {
        final Object r_;
        final Resolver<Object> resolver;
        final Class<?> type = proxy.getClass();
        final Class<?> R =
            Typedef.raw(Typedef.bound(method.getGenericReturnType(), type));
        if (void.class == R || Void.class == R) {
            r_ = null;
            resolver = null;
        } else {
            final Channel<Object> x = _.defer();
            r_ = _.cast(R, x.promise);
            resolver = x.resolver;
        }
        final String base = URI.resolve(here, href);
        final String property = HTTP.property(method);
        if (null != property) {
            get(resolver, base, property, type, method);
        } else {
            // TODO: implement pipeline references?
            post(resolver, base, method.getName(), type, method,
                 ConstArray.array(null == arg ? new Object[0] : arg));
        }
        return r_;
    }
    
    private void
    get(final Resolver<Object> resolver, final String href, final String name,
        final Class<?> type, final Method method) {
        class GET extends Operation implements QueryOperation, Serializable {
            static private final long serialVersionUID = 1L;

            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String target = HTTP.get(href, name);
                final String authority = URI.authority(target);
                final String location = Authority.location(authority);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(target),
                    PowerlessArray.array(
                        new Header("Host", location)
                    )), null);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                if (null != resolver) {
                    _.log.got(request, null, null);
                    resolver.resolve(receive(href, response,
                        Typedef.bound(method.getGenericReturnType(), type)));
                }
            }
            
            public void
            reject(final String request, final Exception reason) {
                if (null != resolver) {
                    _.log.got(request, null, null);
                    resolver.reject(reason);
                }
            }
        }
        _.log.sent(msgs.enqueue(new GET()));
    }
    
    private void
    post(final Resolver<Object> resolver, final String href, final String name,
         final Class<?> type, final Method method, final ConstArray<?> argv) {
        class POST extends Operation implements UpdateOperation, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                return serialize(here,export, HTTP.post(href,name,x,w,m), argv);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                if (null != resolver) {
                    _.log.got(request + "-return", null, null);
                    resolver.resolve(receive(href, response,
                        Typedef.bound(method.getGenericReturnType(), type)));
                }
            }
            
            public void
            reject(final String request, final Exception reason) {
                if (null != resolver) {
                    _.log.got(request + "-return", null, null);
                    resolver.reject(reason);
                }
            }
        }
        _.log.sent(msgs.enqueue(new POST()));
    }
    
    private Volatile<Object>
    receive(final String base, final Message<Response> response, final Type R) {
        try {
            return Eventual.promised(deserialize(base, response, R));
        } catch (final BadSyntax e) {
            /*
             * strip out the parsing information to avoid leaking
             * information to the application layer
             */ 
            return new Rejected<Object>((Exception)e.getCause());
        } catch (final Exception e) {
            return new Rejected<Object>(e);
        }
    }
    
    private Object
    deserialize(final String base,
                final Message<Response> m, final Type R) throws Exception {
        if ("200".equals(m.head.status) || "201".equals(m.head.status) ||
            "202".equals(m.head.status) || "203".equals(m.head.status)) {
            if (Header.equivalent(FileType.unknown.name,
                                  m.head.getContentType())) { return m.body; }
            return new JSONDeserializer().run(base, connect,
                ConstArray.array(R), codebase, m.body.asInputStream()).get(0);
        } 
        if ("204".equals(m.head.status) ||
            "205".equals(m.head.status)) { return null; }
        if ("303".equals(m.head.status)) {
            for (final Header h : m.head.headers) {
                if (Header.equivalent("Location", h.name)) {
                    return connect.run(h.value, base, R);
                }
            }
            return null;    // request accepted, but no response provided
        } 
        throw new Failure(m.head.status, m.head.phrase);
    }
    
    static private final Class<?> Inline = Eventual.ref(0).getClass();
    
    static protected Message<Request>
    serialize(final String here, final Exporter export,
              final String target, final ConstArray<?> argv) throws Exception {
        final String contentType;
        final ByteArray content;
        Object arg = argv.length() == 1 ? argv.get(0) : null;
        if (Inline.isInstance(arg)) {
            arg = ((Fulfilled<?>)arg).cast();
        }
        if (arg instanceof ByteArray) {
            contentType = FileType.unknown.name;
            content = (ByteArray)arg;
        } else {
            contentType = FileType.json.name;
            content = new JSONSerializer().run(HTTP.changeBase(here, export,
                                               URI.resolve(target, ".")), argv);
        }
        final String authority = URI.authority(target);
        final String location = Authority.location(authority);
        return new Message<Request>(new Request(
            "HTTP/1.1", "POST", URI.request(target),
            PowerlessArray.array(
                new Header("Host", location),
                new Header("Content-Type", contentType),
                new Header("Content-Length", "" + content.length())
            )), content);        
    }
}
