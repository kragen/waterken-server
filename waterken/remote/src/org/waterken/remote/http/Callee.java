// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.var.Factory;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.snapshot.Snapshot;
import org.waterken.remote.Exports;
import org.waterken.syntax.Serializer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.syntax.json.Java;
import org.waterken.uri.Header;
import org.waterken.uri.Path;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.waterken.vat.Root;
import org.web_send.Entity;

/**
 * Server-side of the HTTP web-amp protocol.
 */
final class
Callee extends Struct implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final Server bootstrap;
    
    private final ClassLoader code;
    private final Exports exports;

    Callee(final Server bootstrap, final Root local) {
        this.bootstrap = bootstrap;

        code = (ClassLoader)local.fetch(null, Root.code);
        exports = new Exports(local);
    }

    // org.waterken.http.Server interface

    public void
    serve(final String resource, final Volatile<Request> requestor,
          final Do<Response,?> respond) throws Exception {

        // further dispatch the request based on the query string
        final String query = URI.query("", resource);
        final String s = Query.arg(null, query, "s");
        final String p = Query.arg(null, query, "p");
        final String m = Query.arg(null, query, "m");

        // check for web browser bootstrap request
        if (null == s) {
            final String project = exports.getProject();
            bootstrap.serve("file:///site/" + URLEncoding.encode(project) + "/",
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
            respond.fulfill(never(request.method));
            return;
        }
        
        // determine the request subject
        Volatile<?> subject;
        try {
            subject = Eventual.promised(exports.use(s));
        } catch (final NullPointerException e) {
            subject = Eventual.promised(Java.reflect(code, s));
        } catch (final Exception e) {
            subject = new Rejected<Object>(e);
        }
        
        // determine the request type
        if (null == p || "*".equals(p)) {   // when block or introspection
            Object value;
            try {
                // AUDIT: call to untrusted application code
                value = subject.cast();
            } catch (final NullPointerException e) {
                respond.fulfill(serialize(request.method, "404", "not yet",
                    ephemeral, Serializer.render, new Rejected<Object>(e)));
                return;
            } catch (final Exception e) {
                value = new Rejected<Object>(e);
            }
            if ("GET".equals(request.method) || "HEAD".equals(request.method)) {
                respond.fulfill(serialize(request.method, "200", "OK", forever,
                    null==p ? Serializer.render : Serializer.describe, value));
            } else {
                final String[] allow = { "TRACE", "OPTIONS", "GET", "HEAD" };
                if ("OPTIONS".equals(request.method)) {
                    respond.fulfill(Request.options(allow));
                } else {
                    respond.fulfill(Request.notAllowed(allow));
                }
            }
            return;
        }                                   // member access

        // to preserve message order, force settling of a promise
        if (!(subject instanceof Fulfilled)) {
            respond.fulfill(never(request.method));
            return;
        }
        // AUDIT: call to untrusted application code
        final Object target = ((Fulfilled)subject).cast();
        
        // prevent access to local implementation details
        if (null == target || Java.isPBC(target.getClass())) {
            respond.fulfill(never(request.method));
            return;
        }
        
        // process the request
        final Member member = Java.dispatch(target.getClass(), p);
        if ("GET".equals(request.method) || "HEAD".equals(request.method)) {
            Object value;
            try {
                final Method lambda = (Method)member;
                if (null == Java.property(lambda)) {
                    throw new ClassCastException();
                }
                // AUDIT: call to untrusted application code
                value = Reflection.invoke(Java.bubble(lambda), target);
            } catch (final Exception e) {
                value = new Rejected<Object>(e);
            }
            final boolean constant = "getClass".equals(member.getName());
            final int maxAge = constant ? forever : ephemeral; 
            final String etag=constant ? null : exports.getTransactionTag();
            Response r = request.hasVersion(etag)
                ? new Response("HTTP/1.1", "304", "Not Modified",
                    PowerlessArray.array(
                        new Header("Cache-Control", "max-age=" + maxAge)
                    ), null)
            : serialize(request.method, "200", "OK",
                        maxAge, Serializer.render, value);
            if (null != etag) { r = r.with("ETag", etag); }
            respond.fulfill(r);
        } else if ("POST".equals(request.method)) {
            final Object value = exports.once(m, member, new Factory<Object>() {
                @Override public Object
                run() {
                    try {
                        final Method lambda = (Method)member;
                        if (null != Java.property(lambda)) {
                            throw new ClassCastException();
                        }
                        final ConstArray<?> argv =
                            deserialize(request, PowerlessArray.array(
                                lambda.getGenericParameterTypes()));

                        // AUDIT: call to untrusted application code
                        return Reflection.invoke(Java.bubble(lambda), target,
                                argv.toArray(new Object[argv.length()]));
                    } catch (final Exception e) {
                        return new Rejected<Object>(e);
                    }
                }
            });
            respond.fulfill(serialize(request.method, "200", "OK",
                                      ephemeral, Serializer.render, value));
        } else {
            final String[] allow = member instanceof Method
                ? (null == Java.property((Method)member)
                    ? new String[] { "TRACE", "OPTIONS", "POST" }
                : new String[] { "TRACE", "OPTIONS", "GET", "HEAD" })
            : new String[] { "TRACE", "OPTIONS" };
            if ("OPTIONS".equals(request.method)) {
                respond.fulfill(Request.options(allow));
            } else {
                respond.fulfill(Request.notAllowed(allow));
            }
        }
    }
    
    /**
     * Constructs a 404 response.
     */
    private Response
    never(final String method) throws Exception {
        return serialize(method, "404", "never", forever, Serializer.render,
                         new Rejected<Object>(new NullPointerException()));
    }
    
    private Response
    serialize(final String method,
              final String status, final String phrase, final int maxAge,
              final boolean describe, final Object value) throws Exception {
        if (value instanceof Entity) {
            final ByteArray content = ((Entity)value).content;
            return new Response("HTTP/1.1", status, phrase,
                PowerlessArray.array(
                    new Header("Cache-Control", "max-age=" + maxAge),
                    new Header("Content-Type", ((Entity)value).type),
                    new Header("Content-Length", "" + content.length())
                ),
                "HEAD".equals(method) ? null : new Snapshot(content));
        }
        final Snapshot body = Snapshot.snapshot(1024, new JSONSerializer().run(
            describe, exports.reply(), ConstArray.array(value)));
        return new Response("HTTP/1.1", status, phrase,
            PowerlessArray.array(
                new Header("Cache-Control", "max-age=" + maxAge),
                new Header("Content-Type", AMP.contentType),
                new Header("Content-Length", "" + body.content.length())
            ),
            "HEAD".equals(method) ? null : body);
    }
    
    private ConstArray<?>
    deserialize(final Request request,
                final PowerlessArray<Type> parameters) throws Exception {
        final String contentType = request.getContentType();
        final ByteArray content = ((Snapshot)request.body).content;
        if (!AMP.contentType.equalsIgnoreCase(contentType)) {
            return ConstArray.array(new Entity(contentType, content));
        }
        final String base = request.base(exports.getHere());
        return new JSONDeserializer().run(base, exports.connect(base), code,
                                          content.asInputStream(), parameters);
    }
}
