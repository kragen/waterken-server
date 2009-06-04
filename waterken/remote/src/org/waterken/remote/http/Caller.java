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
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Failure;
import org.ref_send.promise.Local;
import org.ref_send.promise.Resolver;
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
        class When extends Operation implements Serializable {
            static private final long serialVersionUID = 1L;

            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String requestURI = HTTP.get(URI.resolve(here,href),null);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(requestURI),
                    PowerlessArray.array(
                        new Header("Host",
                            Authority.location(URI.authority(requestURI)))
                    )), null);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                // TODO: implement polling on a 404 response?
                
                final String absolute = URI.resolve(here, href);
                final Object x = receive(HTTP.get(absolute, null),
                                         response, Local.parameter(observer));
                if (Eventual.ref(x) instanceof Remote &&
                        !absolute.equals(URI.resolve(here, export.run(x)))) {
                    _.log.got(request, null, null);
                    _.when(x, observer);
                    return;
                }
                resolve(request, x);
            }
            
            public void
            reject(final String request, final Exception reason) {
                resolve(request, Eventual.reject(reason));
            }
            
            private void
            resolve(final String request, final Object value) {
                HTTP.sample(value, observer, _.log, request);
            }
        }
        _.log.sent(msgs.enqueue(new When()));
    }
   
    public Object
    invoke(final String href, final Object proxy,
           final Method method, final Object... arg) {
        final Channel<Object> r = _.defer();
        final String property = HTTP.property(method);
        if (null != property) {
            get(href, property, proxy, method, r.resolver);
        } else {
            final Class<?> Fulfilled = Eventual.ref(0).getClass();
            final ConstArray.Builder<Object> argv =
                ConstArray.builder(null != arg ? arg.length : 0);
            for (final Object x : null != arg ? arg : new Object[0]) {
                argv.append(Fulfilled.isInstance(x) ? Eventual.near(x) : x);
            }
            post(href,method.getName(),proxy,method,argv.snapshot(),r.resolver);
            // TODO: implement pipeline references?
        }
        return Eventual.cast(Typedef.raw(Typedef.bound(
                method.getGenericReturnType(), proxy.getClass())), r.promise);
    }
    
    private void
    get(final String href, final String name, final Object proxy,
            final Method method, final Resolver<Object> resolver) {
        class GET extends Operation implements QueryOperation, Serializable {
            static private final long serialVersionUID = 1L;

            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String requestURI = HTTP.get(URI.resolve(here,href),name);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(requestURI),
                    PowerlessArray.array(
                        new Header("Host",
                            Authority.location(URI.authority(requestURI)))
                    )), null);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                _.log.got(request, null, null);
                final Object r;
                if ("404".equals(response.head.status)) {
                    // re-dispatch invocation on resolved value of web-key
                    final Do<Object,Object> invoke = Local.curry(method, null);
                    r = _.when(proxy, invoke);
                } else {
                    r = receive(HTTP.get(URI.resolve(here,href),name), response,
                            Typedef.bound(method.getGenericReturnType(),
                                          proxy.getClass()));
                }
                if (null != resolver) { resolver.apply(r); }
            }
            
            public void
            reject(final String request, final Exception reason) {
                _.log.got(request, null, null);
                if (null != resolver) { resolver.reject(reason); }
            }
        }
        _.log.sent(msgs.enqueue(new GET()));
    }
    
    private void
    post(final String href, final String name,
         final Object proxy, final Method method,
         final ConstArray<?> argv, final Resolver<Object> resolver) {
        class POST extends Operation implements UpdateOperation, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                return serialize(here, export,
                    HTTP.post(URI.resolve(here, href), name, x, w, m),
                    ConstArray.array(method.getGenericParameterTypes()), argv);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                final Type R = Typedef.bound(method.getGenericReturnType(),
                                             proxy.getClass());
                final Object r;
                if ("404".equals(response.head.status)) {
                    // re-dispatch invocation on resolved value of web-key
                    _.log.got(request, null, null);
                    final Do<Object,Object> invoke = Local.curry(method, argv);
                    r = _.when(proxy, invoke);
                } else {
                    r = receive(
                        HTTP.post(URI.resolve(here, href), name, null, 0, 0),
                        response, R);
                    if (null != r || (void.class != R && Void.class != R)) {
                        _.log.got(request + "-return", null, null);
                    }
                }
                if (null != resolver &&
                        !(null == r && (void.class == R || Void.class == R))) {
                    resolver.apply(r);
                }
            }
            
            public void
            reject(final String request, final Exception reason) {
                _.log.got(request, null, null);
                if (null != resolver) { resolver.reject(reason); }
            }
        }
        _.log.sent(msgs.enqueue(new POST()));
    }
    
    private Object
    receive(final String base, final Message<Response> m, final Type R) {
        try {
            if ("200".equals(m.head.status) || "201".equals(m.head.status) ||
                "202".equals(m.head.status) || "203".equals(m.head.status)) {
                String contentType = m.head.getContentType();
                if (null == contentType) {
                    contentType = FileType.json.name;
                } else {
                    final int end = contentType.indexOf(';');
                    if (-1 != end) {
                        contentType = contentType.substring(0, end);
                    }
                }
                return Header.equivalent(FileType.json.name, contentType) ?
                    new JSONDeserializer().deserialize(base, connect, R,
                            codebase, m.body.asInputStream()) : m.body;
            } 
            if ("204".equals(m.head.status) ||
                "205".equals(m.head.status)) { return true; }
            if ("303".equals(m.head.status)) {
                for (final Header h : m.head.headers) {
                    if (Header.equivalent("Location", h.name)) {
                        return connect.run(h.value, base, R);
                    }
                }
            } 
            throw new Failure(m.head.status, m.head.phrase);
        } catch (final BadSyntax e) {
            /*
             * strip out the parsing information to avoid leaking
             * information to the application layer
             */ 
            return Eventual.reject((Exception)e.getCause());
        } catch (final Exception e) {
            return Eventual.reject(e);
        }
    }
    
    static protected Message<Request>
    serialize(final String here, final Exporter export, final String requestURI,
              final ConstArray<Type> types,
              final ConstArray<?> argv) throws Exception {
        final String contentType;
        final ByteArray content;
        if (argv.length() == 1 && argv.get(0) instanceof ByteArray) {
            contentType = FileType.unknown.name;
            content = (ByteArray)argv.get(0);
        } else {
            contentType = FileType.json.name;
            content = new JSONSerializer().serializeTuple(
                HTTP.changeBase(here, export, URI.resolve(requestURI, ".")),
                types, argv);
        }
        return new Message<Request>(new Request(
            "HTTP/1.1", "POST", URI.request(requestURI),
            PowerlessArray.array(
              new Header("Host", Authority.location(URI.authority(requestURI))),
              new Header("Content-Type", contentType),
              new Header("Content-Length", "" + content.length())
            )), content);        
    }
}
