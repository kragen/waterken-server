// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.security.SecureRandom;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.charset.URLEncoding;
import org.joe_e.reflect.Reflection;
import org.ref_send.log.Event;
import org.ref_send.log.Got;
import org.ref_send.log.Sent;
import org.ref_send.log.Trace;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.var.Factory;
import org.waterken.base32.Base32;
import org.waterken.remote.base.Base;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.waterken.vat.Effect;
import org.waterken.vat.Root;
import org.waterken.vat.Tracer;

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
    getHere() { return local.fetch("x-browser:", Root.here); }
    
    /**
     * Gets the project name for this vat.
     */
    public String
    getProject() { return local.fetch(null, Root.project); }

    /**
     * Calls {@link Root#getTransactionTag()}.
     */
    public String
    getTransactionTag() { return local.getTransactionTag(); }
    
    /**
     * Logs a POST request send.
     * @param mid   request message identifier
     */
    public void
    sent(final String mid) {
        
        // determine if logging is turned on
        final Receiver<Event> er = events();
        if (null == er) { return; }

        // output a log event
        final Tracer tracer = local.fetch(null, Root.tracer);
        log(er, new Sent(local.anchor(), null!=tracer?tracer.get():null,
                         local.pipeline(local.pipeline(mid)))); 
    }
    
    /**
     * Logs receipt of an HTTP response.
     * @param mid   request message identifier
     */
    public void
    received(final String mid) {
        
        // determine if logging is turned on
        final Receiver<Event> er = events();
        if (null == er) { return; }

        // output a log event
        final Tracer tracer = local.fetch(null, Root.tracer);
        log(er, new Got(local.anchor(), null != tracer ? tracer.get() : null,
                        local.pipeline(local.pipeline(local.pipeline(mid))))); 
    }
    
    /**
     * Does an operation at most once.
     * @param mid       message identifier
     * @param member    member to invoke, or <code>null</code> if unspecified
     * @param invoke    member invoker
     * @return <code>invoke</code> return value
     */
    public Object
    once(final String mid, final Member member, final Factory<Object> invoke) {
        if (null == mid) { return invoke.run(); }
        
        final String pipe = local.pipeline(mid);
        final Token pumpkin = new Token();
        Object r = local.fetch(pumpkin, pipe);
        if (pumpkin == r) {
            final Receiver<Event> er = events();
            if (null != er) {
                final Tracer tracer = local.fetch(null, Root.tracer);
                final Trace trace = null != tracer
                    ? (null != member ? tracer.dummy(member) : tracer.get())
                : null;
                final String msg = local.pipeline(pipe);
                log(er, new Got(local.anchor(), trace, msg)); 
                r = invoke.run();
                log(er, new Sent(local.anchor(), trace, local.pipeline(msg)));
            } else {
                r = invoke.run();
            }
            local.link(pipe, r);
        }
        return r;
    }
    
    private Receiver<Event>
    events() {
        final Factory<Receiver<Event>> erf = local.fetch(null, Root.events);
        return null != erf ? erf.run() : null;
    }
    
    private void
    log(final Receiver<Event> er, final Event e) {
        final Loop<Effect> effect = local.fetch(null,Root.effect); 
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
            run(final String href, final String base, final Type type) {
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
        return Base.relative(base, Base.absolute(getHere(), export()));
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
                    (null == object || isPBC(object.getClass()) ||
                        !(Eventual.promised(object) instanceof Fulfilled)
                            ? "&src=" : ""); 
            }
        }
        return Remote.bind(local, new ExporterX());
    }
    
    /**
     * Produces a promise for the server-side copy of a return value.
     * @param base      base URL for the server
     * @param mid       message identifier
     * @param R         return type
     * @param response  client-side copy of a return promise
     * @return remote reference to the server-side copy of the return value
     */
    public Object
    far(final String base, final String mid,
        final Class<?> R, final Promise<?> response) {
        final String here = local.fetch(null, Root.here);
        if (null == here) {
            final Eventual _ = local.fetch(null, Remoting._);
            return _.cast(R, response);
        }
        final String pipe = local.pipeline(mid);
        local.link(pipe, response);
        return Remote._(R, local, URI.resolve(base, "#" + pipe +
            "&src=" + URLEncoding.encode(URI.relate(base, here))));
    }

    /**
     * Generates a message identifier.
     */
    public String
    mid() {
        final byte[] secret = new byte[128 / Byte.SIZE];
        final SecureRandom prng = local.fetch(null, Root.prng);
        prng.nextBytes(secret);
        return Base32.encode(secret);
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
     * Is the given web-key a pipeline web-key?
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
     * Extracts the source vat URL from a pipeline web-key.
     * @param URL   web-key
     * @return source vat URL, or <code>null</code> if <code>URL</code> is not
     *         a pipeline web-key
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
     * Is the given type a pass-by-construction type?
     * @param type  candidate type
     * @return <code>true</code> if pass-by-construction,
     *         else <code>false</code>
     */
    static public boolean
    isPBC(final Class<?> type) {
        return String.class == type ||
            Void.class == type ||
            Integer.class == type ||
            Long.class == type ||
            Boolean.class == type ||
            java.math.BigInteger.class == type ||
            Byte.class == type ||
            Short.class == type ||
            Character.class == type ||
            Double.class == type ||
            Float.class == type ||
            java.math.BigDecimal.class == type ||
            org.web_send.Entity.class == type ||
            org.ref_send.Record.class.isAssignableFrom(type) ||
            Throwable.class.isAssignableFrom(type) ||
            org.joe_e.array.ConstArray.class.isAssignableFrom(type) ||
            org.ref_send.promise.Volatile.class.isAssignableFrom(type) ||
            java.lang.reflect.Type.class.isAssignableFrom(type) ||
            java.lang.reflect.AnnotatedElement.class.isAssignableFrom(type);
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
     * Finds a named instance member.
     * @param type  class to search
     * @param name  member name
     * @return corresponding member, or <code>null</code> if not found
     */
    static public Method
    dispatch(final Class<?> type, final String name) {
        Method r = null;
        for (final Method m : Reflection.methods(type)) {
            final int flags = m.getModifiers();
            if (!isStatic(flags) && !isSynthetic(flags)) {
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
        if (isPublic(declarer.getModifiers())) { return method; }
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
