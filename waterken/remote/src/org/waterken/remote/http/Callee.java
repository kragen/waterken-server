// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

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
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.id.Importer;
import org.waterken.id.exports.Exports;
import org.waterken.io.buffer.Buffer;
import org.waterken.io.snapshot.Snapshot;
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
import org.web_send.Entity;
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

        // check for web browser bootstrap request
        if (null == s) {
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
            respond.fulfill(request.trace());
            return;
        }
        
        // check that there is no path name
        if (!"".equals(Path.name(URI.path(resource)))) {
            respond.fulfill(notFound(request.method, forever));
            return;
        }
        
        // determine the request subject
        final Volatile<?> subject; {
            final Token pumpkin = new Token();
            Object x = exports.use(pumpkin, s);
            if (pumpkin == x) {
                x = Java.reflect((ClassLoader)local.fetch(null, Root.code), s);
            }
            subject = Eventual.promised(x);
        }
        
        // determine the request type
        if (null == p || "*".equals(p)) {   // when block or introspection
            Object value;
            try {
                // AUDIT: call to untrusted application code
                value = subject.cast();
            } catch (final NullPointerException e) {
                respond.fulfill(serialize(request.method, "404", "not yet",
                    ephemeral, Serializer.render, new Rejected(e)));
                return;
            } catch (final Exception e) {
                value = new Rejected(e);
            }
            if ("GET".equals(request.method) || "HEAD".equals(request.method)) {
                respond.fulfill(serialize(request.method, "200", "OK", forever,
                    null==p ? Serializer.render : Serializer.describe, value));
            } else {
                respond.fulfill(request.respond("TRACE, OPTIONS, GET, HEAD"));
            }
            return;
        }                                   // member access

        // to preserve message order, force settling of a promise
        if (!(subject instanceof Fulfilled)) {
            respond.fulfill(notFound(request.method, forever));
            return;
        }
        // AUDIT: call to untrusted application code
        final Object target = ((Fulfilled)subject).cast();
        
        // prevent access to local implementation details
        if (null == target || Java.isPBC(target.getClass())) {
            respond.fulfill(notFound(request.method, forever));
            return;
        }
        
        // determine the requested member
        final Member member = Java.dispatch(target.getClass(), p);
        if (member instanceof Method) {
            final Method lambda = (Method)member;
            if (null != Java.property(lambda)) {
                if ("GET".equals(request.method) ||
                        "HEAD".equals(request.method)) {
                    Object value;
                    try {
                        // AUDIT: call to untrusted application code
                        value = Reflection.invoke(lambda, target);
                    } catch (final Exception e) {
                        value = new Rejected(e);
                    }
                    respond.fulfill(serialize(request.method, "200", "OK",
                            ephemeral, Serializer.render, value));
                } else {
                  respond.fulfill(request.respond("TRACE, OPTIONS, GET, HEAD"));
                }
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
                respond.fulfill(serialize(request.method, "200", "OK",
                                          ephemeral, Serializer.render, value));
            } else {
                respond.fulfill(request.respond("TRACE, OPTIONS, POST"));
            }
        } else if (member instanceof Field) {
            final Field field = (Field)member;
            final boolean constant = Modifier.isFinal(field.getModifiers());
            if ("GET".equals(request.method) || "HEAD".equals(request.method)) {
                final Object value = Reflection.get(field, target);
                respond.fulfill(serialize(request.method, "200", "OK",
                    constant ? forever : ephemeral, Serializer.render, value));
            } else if ("POST".equals(request.method) && !constant) {
                final Object arg = deserialize(request,
                        PowerlessArray.array(field.getGenericType()));
                Reflection.set(field, target, arg);
                respond.fulfill(serialize(request.method, "200", "OK",
                                          ephemeral, Serializer.render, null));
            } else {
                respond.fulfill(request.respond(constant
                    ? "TRACE, OPTIONS, GET, HEAD"
                : "TRACE, OPTIONS, GET, HEAD, POST"));
            }
        } else {
            respond.fulfill(request.respond("TRACE, OPTIONS"));
        }
    }
    
    /**
     * Constructs a 404 response.
     */
    private Response
    notFound(final String method, final int maxAge) throws Exception {
        return serialize(method, "404", forever == maxAge ? "never" : "not yet",
          maxAge, Serializer.render, new Rejected(new NullPointerException()));
    }
    
    private Response
    serialize(final String method,
              final String status, final String phrase, final int maxAge,
              final boolean describe, final Object value) throws Exception {
        if (value instanceof Entity) {
            return new Response("HTTP/1.1", status, phrase,
                PowerlessArray.array(
                    new Header("Cache-Control", "max-age=" + maxAge),
                    new Header("Content-Type", ((Entity)value).type),
                    new Header("Content-Length",
                               "" + ((Entity)value).content.length())
                ),
                "HEAD".equals(method)
                    ? null
                : new Snapshot(((Entity)value).content));
        }
        final Buffer content = Buffer.copy(
            new JSONSerializer().run(describe, Java.bind(ID.bind(Remote.bind(
                local, Exports.bind(local)))), ConstArray.array(value)));
        return new Response("HTTP/1.1", status, phrase,
            PowerlessArray.array(
                new Header("Cache-Control", "max-age=" + maxAge),
                new Header("Content-Type", AMP.contentType),
                new Header("Content-Length", "" + content.length)
            ),
            "HEAD".equals(method) ? null : content);        
    }
    
    private ConstArray<?>
    deserialize(final Request request,
                final PowerlessArray<Type> parameters) throws Exception {
        final String contentType = request.getContentType();
        if (!AMP.contentType.equalsIgnoreCase(contentType)) {
            return ConstArray.array(new Entity(contentType, Snapshot.snapshot(
                    (int)((Buffer)request.body).length, request.body)));
        }
        final String here = (String)local.fetch(null, Remoting.here);
        final ClassLoader code = (ClassLoader)local.fetch(null, Root.code);
        final Importer connect = Exports.use(here, exports,
            Java.use(here, code, ID.use(here, Remote.use(local)))); 
        return new JSONDeserializer().run(here, connect, code,
                ((Buffer)request.body).open(), parameters);
    }
}
