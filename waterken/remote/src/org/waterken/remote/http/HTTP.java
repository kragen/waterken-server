// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static java.lang.reflect.Modifier.isStatic;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.inert;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Local;
import org.ref_send.promise.Log;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.ref_send.promise.Vat;
import org.ref_send.type.Typedef;
import org.waterken.db.Creator;
import org.waterken.db.Database;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.TransactionMonitor;
import org.waterken.http.Server;
import org.waterken.remote.Messenger;
import org.waterken.remote.Remote;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.Header;
import org.waterken.uri.Query;
import org.waterken.uri.URI;

/**
 * A web-key interface to a {@link Root}.
 */
public final class
HTTP extends Eventual implements Serializable {
    static private final long serialVersionUID = 1L;    
    
    private   final Root root;                          // vat root
    private   final Promise<Outbound> outbound;         // active msg pipelines
    
    private   final Creator creator;                    // sub-vat factory
    private   final Receiver<Effect<Server>> effect;    // tx effect scheduler

    private
    HTTP(final Receiver<Promise<?>> enqueue,
         final String here, final Log log, final Receiver<?> destruct,
         final Root root, final Promise<Outbound> outbound) {
        super(new Token(), enqueue, here, log, destruct);
        this.root = root;
        this.outbound = outbound;
        
        creator = root.fetch(null, Database.creator);
        effect = root.fetch(null, Database.effect);
    }
    
    // org.ref_send.promise.Eventual interface

    public @Override @SuppressWarnings("unchecked") <R> Vat<R>
    spawn(final String label, final Class<?> maker, final Object... argv) {
        Method make = null;
        try {
            for (final Method m : Reflection.methods(maker)) {
                if ("make".equals(m.getName()) &&
                        Modifier.isStatic(m.getModifiers())) {
                    make = m;
                    break;
                }
            }
            if (null == make) { throw new NullPointerException(); }
        } catch (final Exception e) { throw new Error(e); }
        final Class<?> R = make.getReturnType();
        try {
            final Exports http = new Exports(this);
            final ByteArray body = new JSONSerializer().run(
                    http.export(), ConstArray.array(argv)); 
            final PowerlessArray<String> rd = creator.run(null, here, label,
                new VatInitializer(make, here, body)).call();
            log.sent(rd.get(0));
            final Importer connect = http.connect();
            return new Vat(
                (R)connect.run(rd.get(1), here, R),
                (Receiver<?>)connect.run(rd.get(2), here, Receiver.class)
            );
        } catch (final Exception e) {
            try {
                final Receiver<?> destruct = cast(Receiver.class, null);
                return new Vat(cast(R, reject(e instanceof BadSyntax
                        ? (Exception)e.getCause() : e)), destruct);
            } catch (final Exception ee) { throw new Error(ee); }
        }
    }
    
    // org.waterken.remote.http.Exports interface
    
    static protected HTTP.Exports
    make(final Receiver<Promise<?>> enqueue, final Root root, final Log log,
         final Receiver<?> destruct, final Promise<Outbound> outbound) {
        final String here = root.fetch(null, Database.here);
        return new Exports(new HTTP(enqueue,here,log, destruct,root,outbound));
    }
    
    static public final class
    Exports extends Struct implements Messenger, Serializable {
        static private final long serialVersionUID = 1L;
        
        public final HTTP _;
        private final TransactionMonitor tagger;
        
        private
        Exports(final HTTP _) {
            this._ = _;
            tagger = _.root.fetch(null, Database.monitor);
        }
        
        // org.waterken.remote.Messenger interface

        public void
        when(final String href, final Remote proxy,
             final Do<Object,?> observer) {
            if (isPromise(URI.fragment("", href))) {
                peer(href).when(href, proxy, observer);
            } else {
                // TODO: do this via the peer object to keep order
                final Class<?> P = Typedef.raw(Local.parameter(observer));
                _.when(P, HTTP.inline(Eventual.cast(P, proxy)), observer);
            }
        }
        
        public Object
        invoke(final String href, final Object proxy,
               final Method method, final Object... arg) {
            if (isPromise(URI.fragment("", href))) {
                // re-dispatch invocation on resolved value of web-key
                final ConstArray<?> argv =
                    ConstArray.array(null == arg ? new Object[0] : arg);
                final Do<Object,Object> invoke = Local.curry(method, argv);
                return _.when(proxy, invoke);
            } else {
                return peer(href).invoke(href, proxy, method, arg);
            }
        }
        
        // org.waterken.remote.http.Exports.HTTP interface
        
        private Caller
        peer(final String href) {
            final String peer = URI.resolve(URI.resolve(_.here, href), ".");
            final String peerKey = ".peer-" + URLEncoding.encode(peer);
            Pipeline msgs = _.root.fetch(null, peerKey);
            if (null == msgs) {
                final SessionInfo s = new SessionMaker(_.root).getFresh();
                msgs = new Pipeline(peer, s.sessionKey, s.sessionName,
                                    _.effect, _.outbound);
                _.root.assign(peerKey, msgs);
            }
            return new Caller(_,_.here,getCodebase(), connect(),export(),msgs);
        }
        
        /**
         * Gets the base URL for this URL space.
         */
        public String
        getHere() { return _.here; }
        
        public ClassLoader
        getCodebase() { return _.root.fetch(null, Database.code); }
        
        /**
         * Constructs a reference importer.
         */
        public Importer
        connect() {
            final Importer next=Remote.connect(_, _.local, this, _.here);
            class ImporterX extends Struct implements Importer, Serializable {
                static private final long serialVersionUID = 1L;

                public Object
                run(final String href, final String base,
                                       final Type type) throws Exception {
                    final String URL=null!=base ? URI.resolve(base,href) : href;
                    return Header.equivalent(URI.resolve(URL, "."), _.here)
                        ? reference(URI.fragment("", URL))
                    : next.run(URL, null, type);
                }
            }
            return new ImporterX();
        }
        
        static private final Class<?> Fulfilled = Eventual.ref(0).getClass();

        /**
         * Constructs a reference exporter.
         */
        public Exporter
        export() {
            class ExporterX extends Struct implements Exporter, Serializable {
                static private final long serialVersionUID = 1L;

                public String
                run(final @inert Object object) {
                    final Promise<?> p = Eventual.ref(object);
                    return href(_.root.export(object, false),
                                !Fulfilled.isInstance(p) || isPBC(near(p)));
                }
            }
            return Remote.export(_.local, new ExporterX());
        }

        protected String
        getTransactionTag() { return tagger.tag(); }
        
        /**
         * Receives an operation.
         * @param query     request query string
         * @param method    corresponding operation
         * @param op        operation to run
         * @return <code>op</code> return value
         */
        protected Object
        execute(final String query,
                final Method method, final Promise<Object> op) {
            final String x = session(query);
            if (null == x) { return ServerSideSession.execute(op); }
            final ServerSideSession session = new SessionMaker(_.root).open(x);
            return session.once(window(query), message(query), method, op);
        }
        
        /**
         * Fetches a message target.
         * @param query web-key argument string
         * @return target reference
         */
        protected Object
        reference(final String query) {
            final String s = subject(query);
            return null==s || s.startsWith(".") ? null : _.root.fetch(null, s);
        }
    }
    
    /*
     * web-key parameters
     * x:   message session secret
     * w:   message window number
     * m:   intra-window message number
     * s:   message target key
     * q:   message query, typically the method name
     * o:   present if web-key is a promise
     */
    
    /**
     * Constructs a live web-key for a POST request.
     * @param href          web-key
     * @param predicate     predicate string, or <code>null</code> if none
     * @param sessionKey    message session key
     * @param window        message window number
     * @param message       intra-window message number
     */
    static protected String
    post(final String href, final String predicate,
         final String sessionKey, final long window, final int message) {
        final StringBuilder r = new StringBuilder();
        if (null != predicate) {
            r.append("?q=");
            r.append(URLEncoding.encode(predicate));
        }
        if (null != sessionKey) {
            r.append(0 == r.length() ? '?' : '&');
            r.append("x=");
            r.append(URLEncoding.encode(sessionKey));
            r.append("&w=");
            r.append(window);
            r.append("&m=");
            r.append(message);
        }
        final String query = URI.query("", href);
        if (!"".equals(query)) {
            r.append(0 == r.length() ? '?' : '&');
            r.append(query);
        }
        final String fragment = URI.fragment("", href);
        if (!"".equals(fragment)) {
            r.append(0 == r.length() ? '?' : '&');
            r.append(fragment);
        }
        return URI.resolve(href, r.toString());
    }
    
    /**
     * Constructs a live web-key for a GET request.
     * @param href      web-key
     * @param predicate predicate string, or <code>null</code> if none
     */
    static protected String
    get(final String href, final String predicate) {
        return post(href, predicate, null, 0, 0);
    }
    
    /**
     * Constructs a web-key.
     * @param subject   target object key
     * @param isPromise Is the target object a promise?
     */
    static protected String
    href(final String subject, final boolean isPromise) {
        return "./#"+(isPromise ? "o=&" : "")+"s="+URLEncoding.encode(subject);
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
     * Extracts the session key.
     * @param q web-key argument string
     * @return corresponding session key
     */
    static private String
    session(final String q) { return Query.arg(null, q, "x"); }
    
    static private long
    window(final String q) { return Long.parseLong(Query.arg(null, q, "w")); }
    
    static private int
    message(final String q) { return Integer.parseInt(Query.arg("0", q, "m")); }
    
    /**
     * Is the given object pass-by-construction?
     * @param object  candidate object
     * @return <code>true</code> if pass-by-construction,
     *         else <code>false</code>
     */
    static protected boolean
    isPBC(final Object object) {
        final Class<?> type = null != object ? object.getClass() : Void.class;
        return String.class == type ||
            Integer.class == type ||
            Boolean.class == type ||
            Long.class == type ||
            Byte.class == type ||
            Short.class == type ||
            Character.class == type ||
            Double.class == type ||
            Float.class == type ||
            Void.class == type ||
            java.math.BigInteger.class == type ||
            java.math.BigDecimal.class == type ||
            org.ref_send.scope.Scope.class == type ||
            org.ref_send.Record.class.isAssignableFrom(type) ||
            Throwable.class.isAssignableFrom(type) ||
            org.joe_e.array.ConstArray.class.isAssignableFrom(type) ||
            org.ref_send.promise.Promise.class.isAssignableFrom(type);
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
     * Finds a named property accessor.
     * @param target    invocation target
     * @param name      property name
     * @return corresponding method, or <code>null</code> if not found
     */
    static public Method
    dispatchGET(final Object target, final String name) {
        final Class<?> type = null != target ? target.getClass() : Void.class;
        final boolean c = Class.class == type;
        Method r = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            final int flags = m.getModifiers();
            if (c == isStatic(flags) && !m.isSynthetic() && !m.isBridge()) {
                if (name.equals(property(m))) {
                    if (null != r) { return null; }
                    r = m;
                }
            }
        }
        return r;
    }

    /**
     * Finds a named method.
     * @param target    invocation target
     * @param name      method name
     * @return corresponding method, or <code>null</code> if not found
     */
    static public Method
    dispatchPOST(final Object target, final String name) {
        final Class<?> type = null != target ? target.getClass() : Void.class;
        final boolean c = Class.class == type;
        Method r = null;
        for (final Method m : Reflection.methods(c ? (Class<?>)target : type)) {
            final int flags = m.getModifiers();
            if (c == isStatic(flags) && !m.isSynthetic() && !m.isBridge()) {
                if (name.equals(m.getName()) && null == property(m)) {
                    if (null != r) { return null; }
                    r = m;
                }
            }
        }
        return r;
    }
    
    /**
     * Changes the base URI.
     * @param here      current base URI
     * @param export    local exporter
     * @param there     new base URI
     */
    static protected Exporter
    changeBase(final String here, final Exporter export, final String there) {
        class ChangeBase extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public String
            run(final @inert Object target) {
                final String absolute = URI.resolve(here, export.run(target));
                return null != there ? URI.relate(there, absolute) : absolute;
            }
        }
        return new ChangeBase();
    }
}
