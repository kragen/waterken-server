// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remote;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.uri.Header;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.waterken.vat.Root;
import org.waterken.vat.Vat;

/**
 * A web-key interface to a {@link Root}.
 */
/* package */ final class
Exports extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;    
    
    /**
     * vat root
     */
    private final Root local;

    /**
     * Constructs an instance.
     * @param local vat root
     */
    protected
    Exports(final Root local) {
        this.local = local;
    }
    
    // org.waterken.remote.http.Exports interface
    
    /**
     * Gets the base URL for this namespace.
     */
    protected String
    getHere() { return local.fetch("x-browser:", Vat.here); }

    /**
     * Calls {@link Root#getTransactionTag()}.
     */
    protected String
    getTransactionTag() {
        final Task<String> tagger = local.fetch(null, Vat.tagger);
        try { return tagger.run(); } catch (final Exception e) { return ""; }
    }
    
    /*
     * web-key parameters
     * x:   message session secret
     * w:   message window number
     * m:   intra-window message number
     * i:   message target index
     * s:   message target key
     * q:   message operation identifier, typically the method name
     * o:   present if web-key is a promise
     */
    
    /**
     * Constructs a web-key.
     * @param subject   target object key
     * @param isPromise Is the target object a promise?
     */
    static protected String
    href(final String subject, final boolean isPromise) {
        return "#" + (isPromise ? "o=&" : "") + "s=" + subject;
    }

    /**
     * Extracts the subject key from a web-key.
     * @param q web-key argument string
     * @return corresponding subject key
     */
    static private String
    subject(final String q) { return Query.arg(null, q, "s"); }
    
    /**
     * Extracts the predicate string from a web-key.
     * @param q web-key argument string
     * @return corresponding predicate string
     */
    static protected String
    predicate(final String q) { return Query.arg(null, q, "q"); }
    
    /**
     * Is the given web-key a promise web-key?
     * @param q web-key argument string
     * @return <code>true</code> if a promise, else <code>false</code>
     */
    static protected boolean
    isPromise(final String q) { return null != Query.arg(null, q, "o"); }
    
    /**
     * Extracts the session identifier.
     * @param q web-key argument string
     * @return corresponding session identifier
     */
    static private String
    session(final String q) { return Query.arg(null, q, "x"); }
    
    static private long
    window(final String q) { return Long.parseLong(Query.arg(null, q, "w")); }
    
    static private int
    message(final String q) { return Integer.parseInt(Query.arg("0", q, "m")); }
    
    static private int
    index(final String q) { return Integer.parseInt(Query.arg("0", q, "i")); }
    
    /**
     * Receives an operation.
     * @param query     request query string
     * @param member    corresponding operation
     * @param op        operation to run
     * @return <code>invoke</code> return value
     */
    protected Object
    execute(final String query, final Member member, final Task<Object> op) {
        final String x = session(query);
        if (null == x) {
            try {
                return op.run();
            } catch (final Exception e) { return new Rejected<Object>(e); }
        }
        final ServerSideSession session = local.fetch(null, x);
        return session.once(window(query), message(query), member, op);
    }
    
    /**
     * Fetches a message target.
     * @param query web-key argument string
     * @return target reference
     */
    protected Object
    reference(final String query) {
        final String s = subject(query);
        if (null != s) {return s.startsWith(".") ? null : local.fetch(null, s);}
        final ServerSideSession session = local.fetch(null, session(query));
        return session.get(window(query), index(query));
    }
    
    /**
     * Constructs a reference importer.
     */
    protected Importer
    connect() {
        final Eventual _ = local.fetch(null, "._");
        final Token deferred = local.fetch(null, ".deferred");
        final Messenger messenger = local.fetch(null, ".messenger");
        final String here = getHere();
        final Importer next = Remote.connect(_, deferred, messenger, here);
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;

            public Object
            run(final String href, final String base,
                                   final Type type) throws Exception {
                final String URL = null != base ? URI.resolve(base,href) : href;
                return Header.equivalent(URI.resolve(URL, "."), here)
                    ? reference(URI.query(URI.fragment("", URL), URL))
                : next.run(URL, null, type);
            }
        }
        return new ImporterX();
    }
    
    /**
     * Constructs a return argument exporter.
     */
    protected Exporter
    reply() { return export(); }

    /**
     * Constructs a reference exporter.
     */
    private Exporter
    export() {
        class ExporterX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public String
            run(final Object object) {
                return href(local.export(object, false), Remote.isPBC(object) ||
                    !(Eventual.promised(object) instanceof Fulfilled));
            }
        }
        final Messenger to = local.fetch(null, ".messenger");
        return Remote.export(to, new ExporterX());
    }
    
    /**
     * Gets the corresponding property name.
     * <p>
     * This method implements the standard Java beans naming conventions.
     * </p>
     * @param method    candidate method
     * @return name, or null if the method is not a property accessor
     */
    static protected String
    property(final Method method) {
        final String name = method.getName();
        String r =
            name.startsWith("get") &&
            (name.length() == "get".length() ||
             Character.isUpperCase(name.charAt("get".length()))) &&
            method.getParameterTypes().length == 0
                ? name.substring("get".length())
            : (name.startsWith("is") &&
               (name.length() != "is".length() ||
                Character.isUpperCase(name.charAt("is".length()))) &&
               method.getParameterTypes().length == 0
                ? name.substring("is".length())
            : null);
        if (null != r && 0 != r.length() &&
                (1 == r.length() || !Character.isUpperCase(r.charAt(1)))) {
            r = Character.toLowerCase(r.charAt(0)) + r.substring(1);
        }
        return r;
    }
    
    /**
     * synthetic modifier
     */
    static private final int synthetic = 0x1000;
    
    /**
     * Is the synthetic flag set?
     * @param flags Java modifiers
     * @return <code>true</code> if synthetic, else <code>false</code>
     */
    static private boolean
    isSynthetic(final int flags) { return 0 != (flags & synthetic); }

    /**
     * Finds a named method.
     * @param target    invocation target
     * @param name      method name
     * @return corresponding method, or <code>null</code> if not found
     */
    static protected Method
    dispatch(final Object target, final String name) {
        final Class<?> type = null != target ? target.getClass() : Void.class;
        final boolean c = Class.class == type;
        Method r = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            final int flags = m.getModifiers();
            if (c == isStatic(flags) && !isSynthetic(flags)) {
                String mn = property(m);
                if (null == mn) {
                    mn = m.getName();
                }
                if (name.equals(mn)) {
                    if (null != r) { return null; }
                    r = m;
                }
            }
        }
        return r;
    }

    /**
     * Finds the first invocable declaration of a public method.
     */
    static protected Method
    bubble(final Method method) {
        final Class<?> declarer = method.getDeclaringClass();
        if (Object.class == declarer || Struct.class == declarer) {return null;}
        if (isPublic(declarer.getModifiers())) { return method; }
        if (isStatic(method.getModifiers())) { return null; }
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
}
