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
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Deferred;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Failure;
import org.ref_send.promise.Resolver;
import org.ref_send.type.Typedef;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.TokenList;
import org.waterken.io.FileType;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * Client-side of the HTTP web-amp protocol.
 */
/* package */ final class
Caller extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;

    protected final HTTP.Exports exports;
    protected final Pipeline msgs;          // queued HTTP requests
    
    protected
    Caller(final HTTP.Exports exports, final Pipeline msgs) {
        this.exports = exports;
        this.msgs = msgs;
    }

    // org.waterken.remote.http.Caller interface

    public void
    when(final String href, final Class<?> T, final Resolver<Object> resolver) {
        class When extends Operation implements Serializable {
            static private final long serialVersionUID = 1L;
            
            When() { super(false, false); }

            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String requestURI =
                    HTTP.get(URI.resolve(exports.getHere(), href), null);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(requestURI),
                    PowerlessArray.array(
                        new Header("Host",
                            Authority.location(URI.authority(requestURI)))
                    )), null);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                exports._.log.got(request, null, null);
                if ("404".equals(response.head.status)) {
                    resolver.progress();
                } else {
                    receive(href, null, response, T, null, null, resolver);
                }
            }
            
            public void
            reject(final String request, final Exception reason) {
                exports._.log.got(request, null, null);
                resolver.reject(reason);
            }
        }
        exports._.log.sent(msgs.poll(new When()).guid);
    }
   
    public void
    invoke(final String href, final Class<?> type, final Method method,
           ConstArray<?> argv, final Resolver<Object> resolver){
        final String property = Dispatch.property(method);
        if (null != property) {
            get(href, property, type, method, resolver);
            return;
        }
        final Class<?> Fulfilled = Eventual.ref(0).getClass();
        if (null == argv) {
            argv = ConstArray.array();
        } else {
            final ConstArray.Builder<Object> out =
                ConstArray.builder(argv.length());
            for (final Object x : argv) {
                out.append(Fulfilled.isInstance(x) ? Eventual.near(x) : x);
            }
            argv = out.snapshot();
        }
        if (null == resolver) {
            post(href, method.getName(), type, method, argv, null);
            return;
        }
        final Deferred<Object> r = exports._.defer();
        final Pipeline.Position position = 
            post(href, method.getName(), type, method, argv, r.resolver);
        resolver.resolve(exports._.pipeline(r.promise, this, position));
    }
    
    private void
    get(final String href, final String name, final Class<?> type,
            final Method method, final Resolver<Object> resolver) {
        class GET extends Operation implements Serializable {
            static private final long serialVersionUID = 1L;
            
            GET() { super(true, false); }

            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String requestURI =
                    HTTP.get(URI.resolve(exports.getHere(), href), name);
                return new Message<Request>(new Request(
                    "HTTP/1.1", "GET", URI.request(requestURI),
                    PowerlessArray.array(
                        new Header("Host",
                            Authority.location(URI.authority(requestURI)))
                    )), null);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                exports._.log.got(request, null, null);
                receive(href, name, response,
                        type, method, ConstArray.array(), resolver);
            }
            
            public void
            reject(final String request, final Exception reason) {
                exports._.log.got(request, null, null);
                if (null != resolver) { resolver.reject(reason); }
            }
        }
        exports._.log.sent(msgs.enqueue(new GET()).guid);
    }
    
    private Pipeline.Position
    post(final String href, final String name,
         final Class<?> type, final Method method,
         final ConstArray<?> argv, final Resolver<Object> resolver) {
        class POST extends Operation implements Serializable {
            static private final long serialVersionUID = 1L;
            
            POST() { super(false, true); }
            
            public Message<Request>
            render(final String x, final long w, final int m) throws Exception {
                final String here = exports.getHere();
                final String requestURI =
                    HTTP.post(URI.resolve(here, href), name, x, w, m); 
                return serialize(requestURI, HTTP.export(msgs,
                        HTTP.changeBase(here, exports.export(), requestURI)), 
                    ConstArray.array(method.getGenericParameterTypes()), argv);
            }

            public void
            fulfill(final String request, final Message<Response> response) {
                if ("204".equals(response.head.status) ||
                    "205".equals(response.head.status)) { return; }
                if (response.head.status.startsWith("2")) {
                    exports._.log.got(request + "-return", null, null);
                } else {
                    exports._.log.got(request, null, null);
                }
                receive(href, name, response, type, method, argv, resolver);
            }
            
            public void
            reject(final String request, final Exception reason) {
                exports._.log.got(request, null, null);
                if (null != resolver) { resolver.reject(reason); }
            }
        }
        final Pipeline.Position r = msgs.enqueue(new POST()); 
        exports._.log.sent(r.guid);
        return r;
    }
    
    protected void
    receive(final String href, final String name, final Message<Response> m, 
            final Class<?> type, final Method method, final ConstArray<?> argv,
            final Resolver<Object> resolver) {
        try {
            if ("204".equals(m.head.status) || "205".equals(m.head.status)) {
                return;
            }
            if (null != TokenList.find(null, "Warning", m.head.headers)) {
                throw new Warning();
            }
            final String base =
                HTTP.get(URI.resolve(exports.getHere(), href), name);
            if ("300".equals(m.head.status) || "301".equals(m.head.status) ||
                "302".equals(m.head.status) || "307".equals(m.head.status)) {
                final String location =
                    TokenList.find(null, "Location", m.head.headers);
                final Object target = null != location ?
                    exports.connect(null).apply(location, base, type) :
                    deserialize(type, base, m);
                final Object value = null==method ? target : Reflection.invoke(
                    method, target, argv.toArray(new Object[argv.length()]));
                if (null != resolver) { resolver.apply(value); }
                return;
            }
            final Type R = null == method ? type :
                Typedef.bound(method.getGenericReturnType(), type);
            if ("303".equals(m.head.status)) {
                if (null != resolver) {
                    final String location =
                        TokenList.find(null, "Location", m.head.headers);
                    resolver.apply(null != location ?
                        exports.connect(null).apply(location, base, R) :
                        deserialize(R, base, m));
                }
                return;
            } 
            if ("200".equals(m.head.status) || "201".equals(m.head.status) ||
                "202".equals(m.head.status) || "203".equals(m.head.status)) {
                if (null != resolver) {resolver.apply(deserialize(R, base, m));}
                return;
            } 
            throw new Failure(m.head.status, m.head.phrase);
        } catch (final BadSyntax e) {
            /*
             * strip out the parsing information to avoid leaking information to
             * the application layer
             */
            if (null != resolver) { resolver.reject((Exception)e.getCause()); }
        } catch (final Exception e) {
            if (null != resolver) { resolver.reject(e); }
        }
    }
    
    private Object
    deserialize(final Type expected,
                final String base, final Message<Response> m) throws Exception {
        String contentType = m.head.getContentType();
        if (null == contentType) {
            contentType = FileType.json.name;
        } else {
            final int end = contentType.indexOf(';');
            if (-1 != end) {
                contentType = contentType.substring(0, end);
            }
        }
        return Header.equivalent(FileType.unknown.name, contentType) ? m.body :
            new JSONDeserializer().deserialize(m.body.asInputStream(),
                exports.connect(null), base, exports.code, expected);
    }
    
    static protected Message<Request>
    serialize(final String requestURI, final Exporter export,
              final ConstArray<Type> types,
              final ConstArray<?> argv) throws Exception {
        final String contentType;
        final ByteArray content;
        if (argv.length() == 1 && argv.get(0) instanceof ByteArray) {
            contentType = FileType.unknown.name;
            content = (ByteArray)argv.get(0);
        } else {
            contentType = FileType.json.name;
            content = new JSONSerializer().serializeTuple(export, types, argv);
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
