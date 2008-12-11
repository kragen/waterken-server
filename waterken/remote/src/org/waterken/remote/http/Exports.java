// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.SecureRandom;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Reflection;
import org.ref_send.log.CallSite;
import org.ref_send.log.Event;
import org.ref_send.log.Got;
import org.ref_send.log.Sent;
import org.ref_send.log.Trace;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Factory;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.base32.Base32;
import org.waterken.crypto.Encryptor;
import org.waterken.remote.Remote;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.trace.Tracer;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.waterken.vat.Effect;
import org.waterken.vat.Root;
import org.waterken.vat.Vat;

/**
 * A web-key interface to a {@link Root}.
 */
public final class
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
    public
    Exports(final Root local) {
        this.local = local;
    }
    
    /**
     * Gets the base URL for this namespace.
     */
    public String
    getHere() { return local.fetch("x-browser:", Vat.here); }

    /**
     * Calls {@link Root#getTransactionTag()}.
     */
    public String
    getTransactionTag() {
        final Factory<String> tagger = local.fetch(null, Vat.tagger);
        return null != tagger ? tagger.run() : null;
    }
    
    public String
    pipeline(final String m, final long w, final long i) {
        final Encryptor encrypt = local.fetch(null, Vat.AES);
    }
    
    /*
     * message identifiers
     * mid:                     message session key
     * pipe: hash(mid,w,i)      read key for a particular return value
     * qid:  hash(pipe)         POST request identifier
     * rid:  hash(qid)          POST response identifier
     */
    
    /**
     * Does an operation at most once.
     * @param query     message query arguments
     * @param member    member to invoke, or <code>null</code> if unspecified
     * @param op        operation to run
     * @return <code>invoke</code> return value
     */
    public Object
    once(final String query, final Member member, final Factory<Object> op) {
        final String mid = Query.arg(null, query, "m");
        if (null == mid) { return op.run(); }
        
        final String pipe = local.pipeline(mid,
                Long.parseLong(Query.arg("0", query, "w")),
                Long.parseLong(Query.arg("0", query, "i")));
        final Token pumpkin = new Token();
        Object r = local.fetch(pumpkin, pipe);
        if (pumpkin == r) {
            final Receiver<Event> er = events();
            if (null != er) {
                final Tracer tracer = local.fetch(null, Root.tracer);
                final Trace trace = null != tracer && null != member
                    ? tracer.dummy(member)
                : new Trace(PowerlessArray.array(new CallSite[] {}));
                final String qid = local.pipeline(pipe, 0, 0);
                log(er, new Got(local.anchor(), trace, qid));
                
                r = op.run();
                
                log(er, new Sent(local.anchor(), trace,
                                 local.pipeline(qid, 0, 0)));
            } else {
                r = op.run();
            }
            local.link(pipe, r);
        }
        return r;
    }
    
    /**
     * Produces a remote request channel.
     * @param type  return type
     * @param URL   target URL
     * @param mid   message session key
     * @param w     message window number
     * @param i     intra-window message number
     * @return return value channel, or <code>null</code> if none
     */
    public Channel<Object>
    request(final Class<?> type, final String URL,
            final String mid, final long w, final long i) {
        final String pipe = local.pipeline(mid, w, i);
        
        // log the request
        final Receiver<Event> er = events();
        if (null != er) {
            log(er, new Sent(local.anchor(),
                             new Trace(PowerlessArray.array(new CallSite[] {})),
                             local.pipeline(pipe, 0, 0))); 
        }
        
        // build the return value channel
        if (void.class == type || Void.class == type) { return null; }
        final Eventual _ = local.fetch(null, Vat._);        
        final Channel<Object> r = _.defer();
        final String here = local.fetch(null, Root.here);
        if (null == here) { return r; }
        local.link(pipe, r.promise);
        final String base = URI.resolve(URL, ".");
        return new Channel<Object>(
            Remote.make(local, URI.resolve(base,
                "#"+pipe+"&src="+URLEncoding.encode(URI.relate(base, here)))),
            r.resolver);
    }
    
    /**
     * Logs receipt of a remote request response.
     * @param mid   message session key
     * @param w     message window number
     * @param i     intra-window message number
     */
    public void
    response(final String mid, long w, long i) {
        final Receiver<Event> er = events();
        if (null == er) { return; }
        log(er, new Got(local.anchor(),
                        new Trace(PowerlessArray.array(new CallSite[] {})),
                        local.pipeline(local.pipeline(local.pipeline(mid, w, i),
                                                      0, 0), 0, 0))); 
    }
    
    private Receiver<Event>
    events() {
        final Factory<Receiver<Event>> erf = local.fetch(null, Root.events);
        return null != erf ? erf.run() : null;
    }
    
    private void
    log(final Receiver<Event> er, final Event e) {
        final Loop<Effect> effect = local.fetch(null, Root.effect); 
        effect.run(new Effect() { public void run() { er.run(e); } });
    }
    
    /**
     * Fetches an exported reference.
     * @param name  name to lookup
     * @return bound value
     * @throws NullPointerException <code>name</code> is not bound
     */
    public Object
    use(final String name) throws NullPointerException {
        if (name.startsWith(".")) { throw new NullPointerException(); }
        final Token pumpkin = new Token();
        final Object r = local.fetch(pumpkin, name);
        if (pumpkin == r) { throw new NullPointerException(); }
        return r;
    }
    
    /**
     * Constructs a reference importer.
     */
    public Importer
    connect() {
        final Importer next = Remote.use(local);
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;

            public Object
            run(final String href, final String base,
                                   final Type type) throws Exception {
                final String URL = null != base ? URI.resolve(base,href) : href;
                try {
                    if (URI.resolve(URL, ".").equalsIgnoreCase(getHere())) {
                        return use(key(URL));
                    }
                } catch (final Exception e) {}
                return next.run(URL, null, type);
            }
        }
        return new ImporterX();
    }
    
    /**
     * Constructs an invocation argument exporter.
     * @param base  base URL of the recipient
     */
    public Exporter
    send(final String base) {
        return relative(base, export());
    }
    
    /**
     * Constructs a return argument exporter.
     */
    public Exporter
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
                return "#" + local.export(object) +
                    (Remote.isPBC(object) ||
                        !(Eventual.promised(object) instanceof Fulfilled)
                            ? "&src=" : ""); 
            }
        }
        return Remote.bind(local, new ExporterX());
    }

    /**
     * Generates a message identifier.
     */
    public String
    mid() {
        final byte[] secret = new byte[128 / Byte.SIZE];
        final SecureRandom prng = local.fetch(null, Vat.prng);
        prng.nextBytes(secret);
        return Base32.encode(secret);
    }
    
    /**
     * Constructs a relative URI exporter.
     * @param base  base URI
     * @param local local reference exporter
     */
    static private Exporter
    relative(final String base, final Exporter local) {
        class RelativeX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;
    
            public String
            run(final Object target){
                return URI.relate(base, local.run(target));
            }
        }
        return new RelativeX();
    }

    /**
     * Extracts the key from a web-key.
     * @param URL   web-key
     * @return corresponding key
     */
    static public String
    key(final String URL) {
        final String fragment = URI.fragment("", URL);
        final int iArgs = fragment.indexOf('&');
        return iArgs != -1 ? fragment.substring(0, iArgs) : fragment;
    }
    
    /**
     * Is the given web-key a promise web-key?
     * @param URL   web-key
     * @return <code>true</code> if a promise, else <code>false</code>
     */
    static public boolean
    isPromise(final String URL) { return null != arg(URL, "src"); }
    
    /**
     * Converts a web-key to a promise web-key.
     * @param URL   web-key
     * @return promise version of the <code>URL</code>
     */
    static public String
    asPromise(final String URL) {
        return isPromise(URL)
            ? URL
        : URL + (null != URI.fragment(null, URL) ? "" : "#") + "&src=";
    }
    
    /**
     * Extracts the source vat URL from a promise web-key.
     * @param URL   web-key
     * @return source vat URL, or <code>null</code> if <code>URL</code> is not
     *         a promise web-key
     */
    static public String
    src(final String URL) {
        final String src = arg(URL, "src");
        return null == src ? null : URI.resolve(URL, src);
    }

    /**
     * Extracts a web-key argument.
     * @param URL   web-key
     * @param name  parameter name
     * @return argument value, or <code>null</code> if not specified
     */
    static private String
    arg(final String URL, final String name) {
        return Query.arg(null, URI.fragment("", URL), name);
    }
    
    /**
     * Constructs a web-key.
     * @param dst   target vat URL
     * @param key   key
     */
    static public String
    href(final String dst, final String key) {
        return "".equals(key) ? dst : URI.resolve(dst, "#" + key);
    }
    
    /**
     * Gets the corresponding property name.
     * <p>
     * This method implements the standard Java beans naming conventions.
     * </p>
     * @param method    candidate method
     * @return name, or null if the method is not a property accessor
     */
    static public String
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
    static public Method
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
    static public Method
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
