// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.BufferedReader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.UTF8;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Unresolved;
import org.ref_send.scope.Layout;
import org.ref_send.scope.Scope;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.FileType;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.json.JSON;
import org.waterken.syntax.json.JSONParser;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Header;

/**
 * Server-side of the web-key protocol.
 */
/* package */ final class
Callee extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    private final HTTP.Exports exports;

    protected
    Callee(final HTTP.Exports exports) {
        this.exports = exports;
    }
    
    static private final Class<?> Fulfilled = Eventual.ref(0).getClass();

    protected Message<Response>
    run(final String query, final Message<Request> m) throws Exception {
        
        // further dispatch the request based on the accessed member
        final String p = HTTP.predicate(query);
        if (null == p) {                        // introspection or when block
            if ("OPTIONS".equals(m.head.method)) {
                return new Message<Response>(
                    Response.options("TRACE", "OPTIONS", "GET", "HEAD"), null);
            }
            if (!("GET".equals(m.head.method) || "HEAD".equals(m.head.method))){
                return new Message<Response>(
                    Response.notAllowed("TRACE","OPTIONS","GET","HEAD"), null);
            }
            Object value;
            try {
                // AUDIT: call to untrusted application code
                value = Eventual.ref(exports.reference(query)).call();
            } catch (final Unresolved e) {
                return serialize(m.head.method, "404", "not yet",
                                 Server.ephemeral, JSON.Rejected.make(e));
            } catch (final Exception e) {
                value = JSON.Rejected.make(e);
            }
            if (!HTTP.isPromise(query) && !HTTP.isPBC(value)) {
                value = describe(value.getClass());
            }
            final Response failed = m.head.allow("\"\"");
            if (null != failed) { return new Message<Response>(failed, null); }
            return serialize(m.head.method, "200", "OK", Server.forever, value);
        }                                       // member access

        // determine the target object
        final Object target;
        try {
            final Promise<?> subject = Eventual.ref(exports.reference(query));
            // to preserve message order, only access members on a fulfilled ref
            if (!Fulfilled.isInstance(subject)) { throw new Unresolved(); }
            target = subject.call();
            // prevent access to local implementation details
            if (HTTP.isPBC(target)) { throw new Unresolved(); }
        } catch (final Exception e) {
            return serialize(m.head.method, "404", "never", Server.forever, e);
        }

        if ("GET".equals(m.head.method) || "HEAD".equals(m.head.method)) {
            final Method property = bubble(HTTP.dispatchGET(target, p));
            if (null == property) {             // no such property
                final boolean post = null!=bubble(HTTP.dispatchPOST(target, p));
                return new Message<Response>(Response.notAllowed(
                    post ? new String[] { "TRACE", "OPTIONS", "POST" } : 
                           new String[] { "TRACE", "OPTIONS"         }), null);
            }
            Object value;                       // property access
            try {
                // AUDIT: call to untrusted application code
                final Object r = Reflection.invoke(property, target);
                value = Fulfilled.isInstance(r) ? ((Promise<?>)r).call() : r;
            } catch (final Exception e) {
                value = JSON.Rejected.make(e);
            }
            final boolean constant = "getClass".equals(property.getName());
            final int maxAge = constant ? Server.forever : Server.ephemeral; 
            final String etag = constant ? null : exports.getTransactionTag();
            final Response failed = m.head.allow(etag);
            if (null != failed) { return new Message<Response>(failed, null); }
            Message<Response> r =
                serialize(m.head.method, "200", "OK", maxAge, value);
            if (null != etag) {
                r = new Message<Response>(r.head.with("ETag", etag), r.body);
            }
            return r;
        }
        
        if ("POST".equals(m.head.method)) {
            final Method lambda = HTTP.dispatchPOST(target, p);
            final Method declaration = bubble(lambda);
            if (null == declaration) {          // no such method
                final boolean get = null != bubble(HTTP.dispatchGET(target, p));
                return new Message<Response>(Response.notAllowed(
                    get ? new String[] { "TRACE", "OPTIONS", "GET" } : 
                          new String[] { "TRACE", "OPTIONS"        }), null);
            }                                   // method invocation
            final Response failed = m.head.allow(null);
            if (null != failed) { return new Message<Response>(failed, null); }
            final Object value = exports.execute(query, lambda,
                                                 new Promise<Object>(){
                public Object
                call() throws Exception {
                    /*
                     * SECURITY CLAIM: deserialize inside the once block to
                     * ensure application code cannot detect request replay by
                     * causing failed deserialization
                     */ 
                    final ConstArray<?> argv;
                    try {
                        argv = deserialize(m, ConstArray.array(
                                lambda.getGenericParameterTypes()));
                    } catch (final BadSyntax e) {
                        /*
                         * strip out the parsing information to avoid leaking
                         * information to the application layer
                         */ 
                        throw (Exception)e.getCause();
                    }

                    // AUDIT: call to untrusted application code
                    final Object r = Reflection.invoke(declaration, target,
                            argv.toArray(new Object[argv.length()]));
                    return Fulfilled.isInstance(r) ? ((Promise<?>)r).call() : r;
                }
            });
            return serialize(m.head.method,"200","OK", Server.ephemeral, value);
        }
        
        final boolean get = null != bubble(HTTP.dispatchGET(target, p));
        final boolean post = null != bubble(HTTP.dispatchPOST(target, p));
        final String[] allow =
            get && post ? new String[] { "TRACE", "OPTIONS", "GET", "POST" } : 
            get         ? new String[] { "TRACE", "OPTIONS", "GET"         } :
                   post ? new String[] { "TRACE", "OPTIONS",        "POST" } :
                          new String[] { "TRACE", "OPTIONS"                };
        return "OPTIONS".equals(m.head.method) ?
                new Message<Response>(Response.options(allow), null) :
            new Message<Response>(Response.notAllowed(allow), null);
    }
    
    private Message<Response>
    serialize(final String method, final String status, final String phrase,
              final int maxAge, final Object value) throws Exception {
        final String contentType;
        final ByteArray content;
        if (value instanceof ByteArray) {
            contentType = FileType.unknown.name;
            content = (ByteArray)value;
        } else {
            contentType = FileType.json.name;
            content = new JSONSerializer().run(exports.export(), value);
        }
        if ("GET".equals(method) || "HEAD".equals(method)) {
            return new Message<Response>(new Response(
                "HTTP/1.1", status, phrase,
                PowerlessArray.array(
                    new Header("Cache-Control", 
                               0 < maxAge ? "max-age=" + maxAge : "no-cache"),
                    new Header("Content-Type", contentType),
                    new Header("Content-Length", "" + content.length())
                )),
                "HEAD".equals(method) ? null : content);
        }
        return new Message<Response>(new Response(
            "HTTP/1.1", status, phrase,
            PowerlessArray.array(
                new Header("Content-Type", contentType),
                new Header("Content-Length", "" + content.length())
            )), content);
    }
    
    private ConstArray<?>
    deserialize(final Message<Request> m,
                final ConstArray<Type> parameters) throws Exception {
        if (Header.equivalent(FileType.unknown.name, m.head.getContentType())) {
            return ConstArray.array(m.body);
        }
        return new JSONParser(
            exports.getHere(), exports.connect(), exports.getCodebase(),
            new BufferedReader(UTF8.input(m.body.asInputStream()))).
                readTuple(parameters);
    }

    /**
     * Finds the first invokable declaration of a public method.
     */
    static private Method
    bubble(final Method method) {
        if (null == method) { return null; }
        final Class<?> declarer = method.getDeclaringClass();
        if (Object.class == declarer || Struct.class == declarer) {return null;}
        if (Modifier.isPublic(declarer.getModifiers())) { return method; }
        if (Modifier.isStatic(method.getModifiers())) { return null; }
        final String name = method.getName();
        final Class<?>[] param = method.getParameterTypes();
        for (final Class<?> i : declarer.getInterfaces()) {
            try {
                final Method r = bubble(Reflection.method(i, name, param));
                if (null != r) { return r; }
            } catch (final NoSuchMethodException e) {}
        }
        final Class<?> parent = declarer.getSuperclass();
        if (null != parent) {
            try {
                final Method r = bubble(Reflection.method(parent, name, param));
                if (null != r) { return r; }
            } catch (final NoSuchMethodException e) {}
        }
        return null;
    }
    
    static private Scope
    describe(final Class<?> type) {
        return new Layout(PowerlessArray.array("$")).make(JSON.types(type));
    }
}
