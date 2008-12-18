// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.joe_e.array.PowerlessArray.builder;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.array.ArrayBuilder;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.ref_send.scope.Layout;
import org.ref_send.scope.Scope;
import org.waterken.http.Message;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.FileType;
import org.waterken.remote.Remote;
import org.waterken.syntax.json.BadSyntax;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Header;
import org.web_send.Failure;

/**
 * Server-side of the web-key protocol.
 */
/* package */ final class
Callee extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    private final ClassLoader code;
    private final Exports exports;

    Callee(final ClassLoader code, final Exports exports) {
        this.code = code;
        this.exports = exports;
    }

    // org.waterken.http.Server interface

    protected Message<Response>
    run(final String query,
        final Request head, final ByteArray body) throws Exception {
        
        // determine the request subject
        Volatile<?> subject = Eventual.promised(exports.reference(query));
        
        // determine the request type
        final String p = Exports.predicate(query);
        if (null == p || ".".equals(p)) {   // introspection or when block
            Object value;
            try {
                // AUDIT: call to untrusted application code
                value = subject.cast();
            } catch (final NullPointerException e) {
                return serialize(head.method, "404", "not yet",
                                 Server.ephemeral, new Rejected<Object>(e));
            } catch (final Exception e) {
                value = new Rejected<Object>(e);
            }
            final Message<Response> notAllowed =
                head.allow("", "TRACE", "OPTIONS", "GET", "HEAD");
            if (null != notAllowed) { return notAllowed; }
            if (null == p && !Remote.isPBC(value)) {
                value = describe(value.getClass());
            }
            return serialize(head.method, "200", "OK", Server.forever, value);
        }                                   // member access

        // to preserve message order, force settling of a promise
        if (!(subject instanceof Fulfilled)) { throw Failure.notFound(); }
        
        // AUDIT: call to untrusted application code
        final Object target = ((Fulfilled<?>)subject).cast();
        
        // prevent access to local implementation details
        if (Remote.isPBC(target)) { throw Failure.notFound(); }
        
        // process the request
        final Method lambda = Exports.dispatch(target, p);
        if ("GET".equals(head.method) || "HEAD".equals(head.method)) {
            Object value;
            try {
                if (null == lambda || null == Exports.property(lambda)) {
                    throw new NullPointerException();
                }
                // AUDIT: call to untrusted application code
                value = Reflection.invoke(Exports.bubble(lambda), target);
            } catch (final Exception e) {
                value = new Rejected<Object>(e);
            }
            final boolean constant = "getClass".equals(lambda.getName());
            final int maxAge = constant ? Server.forever : Server.ephemeral; 
            final String etag = constant ? "" : exports.getTransactionTag();
            final Message<Response> notAllowed = head.allow(etag);
            if (null != notAllowed) { return notAllowed; }
            Message<Response> r= serialize(head.method,"200","OK",maxAge,value);
            if (!"".equals(etag)) {
                r = new Message<Response>(r.head.with("ETag", etag), r.body);
            }
            return r;
        }
        final Message<Response> notAllowed =
            head.allow(null, "TRACE", "OPTIONS", "GET", "HEAD", "POST");
        if (null != notAllowed) { return notAllowed; }
        final Object value = exports.execute(query, lambda, new Task<Object>() {
            @Override public Object
            run() throws Exception {
                if (null == lambda || null != Exports.property(lambda)) {
                    throw new NullPointerException();
                }
                final ConstArray<?> argv;
                if (Header.equivalent(FileType.unknown.name,
                                         head.getContentType())) {
                    argv = ConstArray.array(body);
                } else {
                    /*
                     * SECURITY CLAIM: deserialize inside the once block to
                     * ensure application code cannot detect request replay by
                     * causing failed deserialization
                     */ 
                    try {
                        argv = deserialize(body, ConstArray.array(
                                lambda.getGenericParameterTypes()));
                    } catch (final BadSyntax e) {
                        /*
                         * strip out the parsing information to avoid leaking
                         * information to the application layer
                         */ 
                        throw (Exception)e.getCause();
                    }
                }

                // AUDIT: call to untrusted application code
                return Reflection.invoke(Exports.bubble(lambda), target,
                        argv.toArray(new Object[argv.length()]));
            }
        });
        return serialize(head.method, "200", "OK", Server.ephemeral, value);
    }
    
    private Message<Response>
    serialize(final String method, final String status, final String phrase,
              final int maxAge, final Object value) throws Exception {
        if (value instanceof ByteArray) {
            final ByteArray content = (ByteArray)value;
            return new Message<Response>(new Response(
                "HTTP/1.1", status, phrase,
                PowerlessArray.array(
                    new Header("Cache-Control", "max-age=" + maxAge),
                    new Header("Content-Type", FileType.unknown.name),
                    new Header("Content-Length", "" + content.length())
                )),
                "HEAD".equals(method) ? null : content);
        }
        final ByteArray content =
            new JSONSerializer().run(exports.reply(), ConstArray.array(value));
        return new Message<Response>(new Response(
            "HTTP/1.1", status, phrase,
            PowerlessArray.array(
                new Header("Cache-Control", "max-age=" + maxAge),
                new Header("Content-Type", FileType.json.name),
                new Header("Content-Length", "" + content.length())
            )),
            "HEAD".equals(method) ? null : content);
    }
    
    private ConstArray<?>
    deserialize(final ByteArray body,
                final ConstArray<Type> parameters) throws Exception {
        return new JSONDeserializer().run(exports.getHere(), exports.connect(),
                parameters, code, body.asInputStream());
    }
    
    static private Scope
    describe(final Class<?> type) {
        final Object ts = types(type);
        return new Scope(new Layout(PowerlessArray.array("$")),
                         ConstArray.array(ts));
    }
    
    /**
     * Enumerate all types implemented by a class.
     */
    static private PowerlessArray<String>
    types(final Class<?> actual) {
        final Class<?> end =
            Struct.class.isAssignableFrom(actual) ? Struct.class : Object.class;
        final PowerlessArray.Builder<String> r = builder(4);
        for (Class<?> i=actual; end!=i; i=i.getSuperclass()) { ifaces(i, r); }
        return r.snapshot();
    }

    /**
     * List all the interfaces implemented by a class.
     */
    static private void
    ifaces(final Class<?> type, final ArrayBuilder<String> r) {
        if (type == Serializable.class) { return; }
        if (Modifier.isPublic(type.getModifiers())) {
            try { r.append(Reflection.getName(type).replace('$', '-')); }
            catch (final Exception e) {}
        }
        for (final Class<?> i : type.getInterfaces()) { ifaces(i, r); }
    }
}
