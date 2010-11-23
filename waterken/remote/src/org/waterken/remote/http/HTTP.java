// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.joe_e.Selfless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Proxies;
import org.ref_send.promise.Deferred;
import org.ref_send.promise.Do;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Log;
import org.ref_send.promise.NotAMaker;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.ref_send.promise.Resolver;
import org.ref_send.promise.Unresolved;
import org.ref_send.promise.Vat;
import org.ref_send.type.Typedef;
import org.waterken.db.Creator;
import org.waterken.db.Database;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.TransactionMonitor;
import org.waterken.http.Server;
import org.waterken.remote.GUID;
import org.waterken.syntax.BadSyntax;
import org.waterken.syntax.Export;
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
    
    protected final Root root;                          // vat root
    protected final Promise<Outbound> outbound;         // active msg pipelines
    
    private   final Creator creator;                    // sub-vat factory
    private   final Receiver<Effect<Server>> effect;    // tx effect scheduler

    private
    HTTP(final Receiver<Promise<?>> enqueue,
         final String here, final Log log, final Receiver<?> destruct,
         final Root root, final Promise<Outbound> outbound) {
        super(enqueue, here, log, destruct);
        this.root = root;
        this.outbound = outbound;
        
        creator = root.fetch(null, Database.creator);
        effect = root.fetch(null, Database.effect);
    }
    
    // org.ref_send.promise.Eventual interface

    public @Override <R> Vat<R>
    spawn(final String label, final Class<?> maker, final Object... argv) {
        final Method make;
        try {
            make = NotAMaker.dispatch(maker);
        } catch (final Exception e) { throw new Error(e); }
        final Class<?> R = make.getReturnType();
        try {
            final Exports exports = new Exports(this);
            final ByteArray body = new JSONSerializer().serializeTuple(
                exports.export(),
                ConstArray.array(make.getGenericParameterTypes()),
                ConstArray.array(argv)); 
            final PowerlessArray<String> rd = creator.apply(null, here, label,
                new VatInitializer(make, here, body)).call();
            log.sent(rd.get(0));
            final Importer connect = exports.connect(null);
            final @SuppressWarnings("unchecked") R top =
                (R)connect.apply(rd.get(1), here, R);
            return new Vat<R>(top,
                (Receiver<?>)connect.apply(rd.get(2), here, Receiver.class)
            );
        } catch (final Exception e) {
            try {
            	final Exception reason =
                    e instanceof BadSyntax ? (Exception)e.getCause() : e;
                final Promise<R> top = reject(reason);
                final Promise<Receiver<?>> ignore = reject(reason);
                return new Vat<R>(cast(R, top), cast(Receiver.class, ignore));
            } catch (final Exception ee) { throw new Error(ee); }
        }
    }
    
    // org.waterken.remote.http.HTTP interface
    
    static protected HTTP.Exports
    make(final Receiver<Promise<?>> enqueue, final Root root, final Log log,
         final Receiver<?> destruct, final Promise<Outbound> outbound) {
        final String here = root.fetch(null, Database.here);
        return new Exports(new HTTP(enqueue,here,log,destruct,root,outbound));
    }
    
    static protected Object
    shorten(final Promise<?> p) throws Exception {
        return p instanceof Local<?> ? ((Local<?>)p).shorten() : p.call();
    }

    /**
     * Do block parameter type
     */
    static protected final TypeVariable<?> P = Typedef.var(Do.class, "P");
    
    /**
     * A remote reference.
     */
    protected final class
    Remote extends Local<Object> {
        static private final long serialVersionUID = 1L;

        /**
         * relative URL string for message target
         */
        protected final String href;

        protected
        Remote(final String href) {
            if (null == href) { throw new NullPointerException(); }
            this.href = href;
        }
        
        // java.lang.Object interface

        /**
         * Is the given object the same?
         * @param x The compared to object.
         * @return true if the same, else false.
         */
        public boolean
        equals(final Object x) {
            return x instanceof Remote &&
                   href.equals(((Remote)x).href) &&
                   HTTP.this.equals(((Remote)x).getScope());
        }
        
        /**
         * Calculates the hash code.
         */
        public int
        hashCode() { return 0x4E307E4F; }

        // org.ref_send.promise.Promise interface

        public Object
        call() throws Exception {
            if (!isPromise(URI.fragment("", href))) { throw new Unresolved(); }
            final String promiseKey = ".promise-" + URLEncoding.encode(href);
            final Promise<?> promise = root.fetch(null, promiseKey);
            if (null == promise) { throw new Unresolved(); }
            return promise.call();
        }
        
        // org.ref_send.promise.Local interface
        
        public Remote
        shorten() { return this; }

        public void
        when(Class<?> T, final Do<Object,?> observer) {
            final Compose<?,?> outer =
                observer instanceof Compose<?,?> ? (Compose<?,?>)observer :null;
            final Do<?,?> inner = null != outer ? outer.conditional : observer;
            if (null == T) {
                // no type information provided, so extract hint from observer
                T = Typedef.raw(inner instanceof Invoke<?> ?
                        ((Invoke<?>)inner).method.getDeclaringClass() :
                        Typedef.value(P, inner.getClass()));
            }
            if (isPromise(URI.fragment("", href))) {
                /*
                 * Queue the observer on a local version of the remote promise,
                 * scheduling a fetch of the remote value when the local version
                 * is first created. Keeping a local version allows us to poll
                 * the remote promise and still ensure all observers are
                 * notified in the same order they were registered.
                 */
                final String promiseKey = ".promise-"+URLEncoding.encode(href);
                Promise<?> promise = root.fetch(null, promiseKey);
                if (null == promise) {
                    final Deferred<Object> local = defer();
                    root.assign(promiseKey, promise = local.promise);
                    peer().when(href, T, local.resolver);
                }
                HTTP.this.when(T, Void.class, promise, observer);
            } else if (inner instanceof Invoke<?>) {
                final Invoke<?> op = (Invoke<?>)inner;
                peer().invoke(href, T, op.method, op.argv,
                              null != outer ? resolver(outer) : null);
            } else {
                /*
                 * The href identifies a remote reference, not a remote promise,
                 * so invoke the observer with an eventual reference.
                 */
                HTTP.this.when(T, Void.class, new Inline<Object>(Proxies.proxy(
                		this, virtualize(T, Selfless.class))), observer);
            }
        }
        
        private Caller
        peer() {
            final String peer = URI.resolve(URI.resolve(here, href), ".");
            final String peerKey = ".peer-" + URLEncoding.encode(peer);
            Pipeline msgs = root.fetch(null, peerKey);
            if (null == msgs) {
                final SessionInfo s = new SessionMaker(root).getFresh();
                msgs = new Pipeline(peer, s.sessionKey, s.sessionName,
                                    enqueue, effect, outbound);
                root.assign(peerKey, msgs);
            }
            return new Caller(new Exports(HTTP.this), msgs);
        }
    }
    
    static protected @SuppressWarnings({"unchecked","rawtypes"})Resolver<Object>
    resolver(final Compose<?,?> outer) { return (Resolver)outer.resolver; }
    
    protected Object
    remote(final String URL, final Type... type) {
        final Remote p = new Remote(URI.relate(here, URL));
        final Class<?>[] types = new Class<?>[type.length + 1];
        types[type.length] = Selfless.class;
        boolean implemented = true;
        for (int i = type.length; 0 != i--;) {
            types[i] = Typedef.raw(type[i]);
            implemented = implemented && types[i].isInstance(p); 
        }
        if (implemented) { return p; }
        return Proxies.proxy(p, virtualize(types));
    }
    
    /**
     * A web-key interface to a {@link Root}.
     */
    static public final class
    Exports extends Struct implements Serializable {
        static private final long serialVersionUID = 1L;    
        
        public final HTTP _;
        protected final ClassLoader code;
        protected final TransactionMonitor tagger;
        
        protected
        Exports(final HTTP _) {
            this._ = _;
            tagger = _.root.fetch(null, Database.monitor);
            code = _.root.fetch(null, Database.code);
        }
        
        /**
         * Gets the base URL for this URL space.
         */
        protected String
        getHere() { return _.here; }
        
        protected ServerSideSession
        getSession(final String query) {
            final String key = session(query);
            if (null == key) { return null; }
            return new SessionMaker(_.root).open(key);
        }
        
        /**
         * Constructs a reference importer.
         */
        public Importer
        connect(final ServerSideSession session) {
            class ImporterX extends Struct implements Importer, Serializable {
                static private final long serialVersionUID = 1L;

                public Object
                apply(final String href, final String base,
                                         final Type... type) throws Exception {
                    final String URL=null!=base ? URI.resolve(base,href) : href;
                    return Header.equivalent(URI.resolve(URL, "."), _.here) ?
                        reference(session, URI.fragment("", URL)) :
                        _.remote(URL, type);
                }
            }
            return GUID.connect(code, _.root, new ImporterX());
        }

        /**
         * Constructs a reference exporter.
         */
        public Exporter
        export() {
            class ExporterX extends Struct implements Exporter, Serializable {
                static private final long serialVersionUID = 1L;

                public Export
                apply(final Object x) {
                    final Promise<?> p = Eventual.ref(x);
                    if (p instanceof Remote && _.trusted(p)) {
                        return new Export(((Remote)p).href);
                    }
                    return new Export(href(_.root.export(x, false),
                            !Fulfilled.isInstance(p) || isPBC(near(p))));
                }
            }
            return GUID.export(code, _.root, new ExporterX());
        }
        
        /**
         * Receives an operation.
         * @param session   messaging session
         * @param query     request query string
         * @param op        operation to run
         * @return <code>op</code> return value
         */
        protected Object
        execute(final ServerSideSession session,
                final String query, final NonIdempotent op) {
            if (null == session) { return op.apply(null); }
            return session.once(window(query), message(query), op);
        }
        
        /**
         * Fetches a message target.
         * @param session   server-side session, or <code>null</code> if none
         * @param query web-key argument string
         * @return target reference
         */
        protected Object
        reference(final ServerSideSession session, final String query) {
            final String s = subject(query);
            if (null != s) {
                return s.startsWith(".") ? null : _.root.fetch(null, s);
            }
            if (null == session) { return null; }
            
            // check for a pipeline reference
            final String p = Query.arg(null, query, "p");
            if (null == p) { return null; } 
            return session.pipeline(Integer.parseInt(p));
        }
    }

    /**
     * A pipelined remote promise.
     */
    protected final class
    PipelinePromise extends Local<Object> {
        static private final long serialVersionUID = 1L;

        private final Local<Object> returned;
        private final Caller caller;
        private final long window;
        private final int message;

        protected
        PipelinePromise(final Local<Object> returned, final Caller caller,
                        final Pipeline.Position position) {
            this.returned = returned;
            this.caller = caller;
            this.window = position.window;
            this.message = position.message;
        }
        
        // java.lang.Object interface

        /**
         * Is the given object the same?
         * @param x The compared to object.
         * @return true if the same, else false.
         */
        public boolean
        equals(final Object x) {
            return x instanceof PipelinePromise &&
                   message == ((PipelinePromise)x).message &&
                   window == ((PipelinePromise)x).window &&
                   caller.equals(((PipelinePromise)x).caller) &&
                   returned.equals(((PipelinePromise)x).returned) &&
                   HTTP.this.equals(((PipelinePromise)x).getScope());
        }
        
        /**
         * Calculates the hash code.
         */
        public int
        hashCode() { return 0x4E307E94; }

        // org.ref_send.promise.Promise interface

        /**
         * Forwards the call to the local copy of the promise.
         */
        public Object
        call() throws Exception { return returned.call(); }
        
        // org.ref_send.promise.Local interface

        public Object
        shorten() throws Unresolved { return returned.shorten(); }
        
        public void
        when(final Class<?> T, final Do<Object,?> observer) {
            if (caller.msgs.canPipeline(window)) {
                final Compose<?,?> outer = observer instanceof Compose<?,?> ?
                        (Compose<?,?>)observer : null;
                final Do<?,?> inner= null!=outer ? outer.conditional : observer;
                if (inner instanceof Invoke<?>) {
                    final Invoke<?> op = (Invoke<?>)inner;
                    if (null != Dispatch.property(op.method)) {
                        class BreakPipeline extends Task {
                            static private final long serialVersionUID = 1L;
                            
                            BreakPipeline() { super(true, false); }

                            public Void
                            call() { returned.when(T, observer); return null; }
                        }
                        caller.msgs.enqueue(new BreakPipeline());
                        return;
                    }
                    caller.invoke(
                        URI.relate(here, caller.msgs.peer + "#p=" + message),
                        null!=T ? T :Typedef.raw(op.method.getDeclaringClass()),
                        op.method, op.argv,
                        null != outer ? resolver(outer) : null);
                    return;
                }
            }
            class FlushPipeline extends Task {
                static private final long serialVersionUID = 1L;
                
                FlushPipeline() { super(false, false); }

                public Void
                call() { returned.when(T, observer); return null; }
            }
            caller.msgs.enqueue(new FlushPipeline());
        }
    }

    protected Promise<Object>
    pipeline(final Promise<Object> returned, final Caller caller,
             final Pipeline.Position position) {
        return new PipelinePromise((Local<Object>)returned, caller, position);
    }
    
    static protected Exporter
    export(final Pipeline msgs, final Exporter next) {
        class ExporterX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Export
            apply(final Object target) {
                final Object handler = target instanceof Proxy
                    ? Proxies.getHandler((Proxy)target) : target;
                if (handler instanceof PipelinePromise) {
                    final PipelinePromise x = (PipelinePromise)handler;
                    if (msgs == x.caller.msgs) {
                        if (x.window == msgs.getActiveWindow()) {
                            return new Export("#p=" + x.message);
                        } else {
                            return new Export(x.returned.shorten());
                        }
                    }
                }
                return next.apply(target);
            }
        }
        return new ExporterX();
    }
    
    /*
     * web-key parameters
     * x:   message session secret
     * w:   message window number
     * m:   intra-window message number
     * p:   pipeline reference
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
        if (!"".equals(fragment) && !fragment.startsWith("=")) {
            final int end = fragment.indexOf("&=");
            final String args = -1==end ? fragment : fragment.substring(0, end);
            if (!"".equals(args)) {
                r.append(0 == r.length() ? '?' : '&');
                r.append(args);
            }
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
     * Does the given web-key refer to a promise? 	 
     * @param q web-key argument string 	 
     * @return <code>true</code> if a promise, else <code>false</code> 	 
     */ 	 
    static protected boolean 	 
    isPromise(final String q) { return null != Query.arg(null, q, "o"); }

    /**
     * Extracts the subject key from a web-key.
     * @param query web-key argument string
     * @return corresponding subject key
     */
    static protected String
    subject(final String query) { return Query.arg(null, query, "s"); }
    
    /**
     * Extracts the predicate string from a web-key.
     * @param method    HTTP request method
     * @param query     web-key argument string
     * @return corresponding predicate string
     */
    static protected String
    predicate(final String method, final String query) {
        return Query.arg("POST".equals(method) ? "apply" : null, query, "q");
    }
    
    /**
     * Extracts the session key.
     * @param query web-key argument string
     * @return corresponding session key
     */
    static protected String
    session(final String query) { return Query.arg(null, query, "x"); }
    
    static protected long
    window(final String q) { return Long.parseLong(Query.arg(null, q, "w")); }
    
    static protected int
    message(final String q) { return Integer.parseInt(Query.arg("0", q, "m")); }
    
    static protected final Class<?> Fulfilled = Eventual.ref(0).getClass();
    
    /**
     * Is the given object pass-by-construction? 	 
     * @param object  candidate object 	 
     * @return <code>true</code> if pass-by-construction, else
     *  	   <code>false</code> 	 
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
     * Changes the base URI.
     * @param here      current base URI
     * @param exporter  local exporter
     * @param there     new base URI
     */
    static protected Exporter
    changeBase(final String here, final Exporter exporter, final String there) {
        class ChangeBase extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public Export
            apply(final Object target) {
                final Export export = exporter.apply(target);
                if (null == export.href) { return export; }
                final String absolute = URI.resolve(here, export.href);
                if (null == there) { return new Export(absolute); }
                return new Export(URI.relate(there, absolute));
            }
        }
        return new ChangeBase();
    }
}
