// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.joe_e.array.ConstArray.array;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.http.Failure;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.id.Importer;
import org.waterken.id.exports.Exports;
import org.waterken.io.Content;
import org.waterken.io.MediaType;
import org.waterken.io.buffer.Buffer;
import org.waterken.model.Root;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.syntax.Serializer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.syntax.json.Java;
import org.waterken.uri.Header;
import org.waterken.uri.Path;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.web_send.graph.Namespace;

/**
 * Server-side of the HTTP web-amp protocol.
 */
final class
Callee extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final Server bootstrap;
    private final Root local;
    private final Namespace exports;

    Callee(final Server bootstrap, final Root local) {
        this.bootstrap = bootstrap;
        this.local = local;
        exports = Exports.make(local);
    }

    // org.waterken.http.Server interface

    @SuppressWarnings("unchecked") public void
    serve(final String resource,
          final Volatile<Request> requestor,
          final Do<Response,?> respond) throws Exception {

        // further dispatch the request based on the query string
        final String query = URI.query("", resource);
        final String s = Query.arg(null, query, "s");
        final String p = Query.arg(null, query, "p");
        final String o = Query.arg(null, query, "o");

        // check for web browser bootstrap request
        if (null == s && null == o) {
            final String project = (String)local.fetch(null, Root.project);
            bootstrap.serve("file:///res/" + URLEncoding.encode(project) + "/",
                            requestor, respond);
            return;
        }

        // determine the request
        final Request request;
        try {
            request = requestor.cast();
        } catch (final Exception e) {
            respond.reject(e);
            return;
        }

        // made it to the final processor, so bounce a TRACE
        if ("TRACE".equals(request.method)) {
            respond.fulfill(new Response(
                "HTTP/1.1", "200", "OK",
                PowerlessArray.array(
                    new Header("Content-Type",
                               "message/http; charset=iso-8859-1")
                ), request));
            return;
        }

        // determine the request style
        final String name = Path.name(URI.path(resource));
        if (null != o && null == s && null == p) {
            final boolean describe;     // introspection request?
            if ("describe".equals(name)) {
                describe = Serializer.describe; // introspection
            } else if ("".equals(name)) {
                describe = Serializer.render;   // when block
            } else {
                respond.reject(Failure.notFound);
                return;
            }
            
            final Object object;        // request target
            final Token pumpkin = new Token();
            final Object x = exports.use(pumpkin, o);
            if (pumpkin == x) {
                final ClassLoader code =
                    (ClassLoader)local.fetch(null, Root.code);
                object = Java.reflect(code, o);
            } else {
                object = x;
            }
            if (null == object) {
                respond.reject(Failure.notFound);
                return;
            }
            
            if ("OPTIONS".equals(request.method)) {
                respond.fulfill(new Response(
                    "HTTP/1.1", "204", "OK",
                    PowerlessArray.array(
                        new Header("Allow", "TRACE, OPTIONS, GET, HEAD")
                    ), null));
                return;
            }
            if (!("GET".equals(request.method) ||
                  "HEAD".equals(request.method))) {
                respond.fulfill(new Response(
                    "HTTP/1.1", "405", "Method Not Allowed",
                    PowerlessArray.array(
                        new Header("Allow", "TRACE, OPTIONS, GET, HEAD"),
                        new Header("Content-Length", "0")
                    ), null));
                return;
            }
            Object value;
            final Volatile<?> pValue = Eventual.promised(object);
            try {
                // AUDIT: call to untrusted application code
                value = pValue.cast();
            } catch (final NullPointerException e) {
                respond.reject(Failure.notFound);
                return;
            } catch (final Exception e) {
                value = new Rejected(e);
            }
            respond.fulfill(new Response(
                "HTTP/1.1", "200", "OK",
                PowerlessArray.array(
                    new Header("Cache-Control", "max-age=" + forever),
                    new Header("Content-Type", MediaType.json.name)
                ), "HEAD".equals(request.method)
                    ? null
                : serialize(describe, value)));
            return;
        }
        if (null == o && null != s && null != p) {
            // script driven invocation
            
            // implementation below...
        } else {
            // unknown request style
            respond.reject(Failure.notFound);
            return;
        }
        
        // check that there is no path name
        if (!"".equals(name)) {
            respond.reject(Failure.notFound);
            return;
        }
        
        // determine the invocation target
        final Object subject = exports.use(null, s);
        if (null == subject) {
            respond.reject(Failure.notFound);
            return;
        }
        
        // to avoid proxying, strip down to a near reference
        final Object target;
        try {
            // AUDIT: call to untrusted application code
            target = Eventual.near(subject);
        } catch (final Exception e) {
            // to preserve message order, force settling of a promise
            respond.reject(Failure.notFound);
            return;
        }
        
        // prevent access to local implementation details
        if (null == target || Java.isPBC(target.getClass())) {
            respond.reject(Failure.notFound);
            return;
        }
        
        // process the request
        final Member member = Java.dispatch(target.getClass(), p);
        if (member instanceof Method) {
            final Method lambda = (Method)member;
            if ("GET".equals(request.method) || "HEAD".equals(request.method)) {
                Object value;
                try {
                    // AUDIT: call to untrusted application code
                    value = Reflection.invoke(lambda, target);
                } catch (final Exception e) {
                    value = new Rejected(e);
                }
                respond.fulfill(new Response(
                    "HTTP/1.1", "200", "OK",
                    PowerlessArray.array(
                        new Header("Cache-Control", "max-age=0"),
                        new Header("Content-Type", MediaType.json.name)
                    ), "HEAD".equals(request.method)
                        ? null
                    : serialize(Serializer.render, value)));
            } else if ("POST".equals(request.method)) {
                final String m = Query.arg(null, query, "m");
                final String pipe = null == m ? null : Exports.pipeline(m);
                final Token pumpkin = new Token();
                Object value = null==pipe ? pumpkin : local.fetch(pumpkin,pipe);
                if (pumpkin == value) {
                    final ConstArray<?> argv = deserialize(request,
                      PowerlessArray.array(lambda.getGenericParameterTypes()));
                    try {
                        // AUDIT: call to untrusted application code
                        value = Reflection.invoke(lambda, target,
                                argv.toArray(new Object[argv.length()]));
                    } catch (final Exception e) {
                        value = new Rejected(e);
                    }
                    if (null != pipe) { local.store(pipe, value); }
                }
                respond.fulfill(new Response(
                    "HTTP/1.1", "200", "OK",
                    PowerlessArray.array(
                        new Header("Content-Type", MediaType.json.name)
                    ), serialize(Serializer.render, value)));
            } else {
                final String allow = null == Java.property(lambda)
                    ? "TRACE, OPTIONS, POST"
                : "TRACE, OPTIONS, GET, HEAD";
                if ("OPTIONS".equals(request.method)) {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "204", "OK",
                        PowerlessArray.array(
                            new Header("Allow", allow)
                        ), null));
                } else {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "405", "Method Not Allowed",
                        PowerlessArray.array(
                            new Header("Allow", allow),
                            new Header("Content-Length", "0")
                        ), null));
                }
            }
        } else if (member instanceof Field) {
            final boolean mutable = !Modifier.isFinal(member.getModifiers());
            final Field field = (Field)member;
            if ("GET".equals(request.method) || "HEAD".equals(request.method)) {
                final Object value = Reflection.get(field, target);
                respond.fulfill(new Response(
                    "HTTP/1.1", "200", "OK",
                    PowerlessArray.array(
                        new Header("Cache-Control",
                                   "max-age=" + (mutable ? 0 : forever)),
                        new Header("Content-Type", MediaType.json.name)
                    ), "HEAD".equals(request.method)
                        ? null
                    : serialize(Serializer.render, value)));
            } else if ("POST".equals(request.method) && mutable) {
                final Object value = deserialize(request,
                        PowerlessArray.array(field.getGenericType()));
                Reflection.set(field, target, value);
                respond.fulfill(new Response(
                    "HTTP/1.1", "200", "OK",
                    PowerlessArray.array(
                        new Header("Content-Type", MediaType.json.name)
                    ), serialize(Serializer.render, null)));
            } else {
                final String allow = mutable
                    ? "TRACE, OPTIONS, GET, HEAD, POST"
                : "TRACE, OPTIONS, GET, HEAD";
                if ("OPTIONS".equals(request.method)) {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "204", "OK",
                        PowerlessArray.array(
                            new Header("Allow", allow)
                        ), null));
                } else {
                    respond.fulfill(new Response(
                        "HTTP/1.1", "405", "Method Not Allowed",
                        PowerlessArray.array(
                            new Header("Allow", allow),
                            new Header("Content-Length", "0")
                        ), null));
                }
            }
        } else {
            respond.reject(Failure.notFound);
        }
    }
    
    private Content
    serialize(final boolean describe, final Object value) {
        return new JSONSerializer().run(describe, Java.bind(ID.bind(
            Remote.bind(local, Exports.bind(local)))), array(value));        
    }
    
    private ConstArray<?>
    deserialize(final Request request,
                final PowerlessArray<Type> parameters) throws Exception {
        if (!MediaType.json.name.equals(request.getContentType())) {
            throw Failure.unsupported;
        }
        final String here = (String)local.fetch(null, Remoting.here);
        final ClassLoader code = (ClassLoader)local.fetch(null, Root.code);
        final Importer connect = Exports.use(here, exports,
            Java.use(here, code, ID.use(here, Remote.use(local)))); 
        return new JSONDeserializer().run(
            here, connect, code, ((Buffer)request.body).open(), parameters);
    }
}
