// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.security.SecureRandom;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.charset.URLEncoding;
import org.ref_send.log.Event;
import org.ref_send.log.Got;
import org.ref_send.log.Sent;
import org.ref_send.log.Trace;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.var.Factory;
import org.ref_send.var.Receiver;
import org.waterken.base32.Base32;
import org.waterken.id.Exporter;
import org.waterken.id.Importer;
import org.waterken.id.base.Base;
import org.waterken.syntax.json.Java;
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
     * @param mid       message identifier,
     *                  or <code>null</code> for idempotent operation
     * @param member    member to invoke
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
                final Trace trace= null != tracer ? tracer.dummy(member) : null;
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
     * @param base  base URL
     */
    public Importer
    connect(final String base) {
        final Importer next = Remote.use(local);
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;

            public Object
            run(final Class<?> type, final String URL) {
                try {
                    if (URI.resolve(URL, ".").equalsIgnoreCase(getHere())) {
                        return use(key(URL));
                    }
                } catch (final Exception e) {}
                return next.run(type, URL);
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
                return (null == object || Java.isPBC(object.getClass()) ||
                        !(Eventual.promised(object) instanceof Fulfilled)
                    ? "?src=" : "") + "#" + local.export(object); 
            }
        }
        return Remote.bind(local, new ExporterX());
    }
    
    /**
     * Produces a promise for the server-side copy of a return value.
     * @param <R> return type
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
        return Remote._(R, local, URI.resolve(base,
            "./?src=" + URLEncoding.encode(URI.relate(base, here)) + "#"+pipe));
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
    key(final String URL) { return URI.fragment("", URL); }
    
    /**
     * Is the given web-key a pipeline web-key?
     * @param URL   web-key
     * @return <code>true</code> if a promise, else <code>false</code>
     */
    static public boolean
    isPromise(final String URL) { return null != arg(URL, "src"); }
    
    /**
     * Extracts the soure vat URL from a pipeline web-key.
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
        return Query.arg(null, URI.query("", URL), name);
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
}
