// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.reflect.Proxies;
import org.joe_e.reflect.Reflection;
import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Compose;
import org.ref_send.promise.eventual.Deferred;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Task;
import org.ref_send.type.Typedef;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.uri.URI;

/**
 * A remote reference.
 */
public final class
Remote extends Deferred<Object> implements Promise<Object> {
    static private final long serialVersionUID = 1L;

    /**
     * network message sender
     */
    private final Messenger messenger;

    /**
     * relative URL string for message target
     */
    private final String href;

    private
    Remote(final Eventual _, final Token deferred,
           final Messenger messenger, final String href) {
        super(_, deferred);
        if (null == messenger) { throw new NullPointerException(); }
        if (null == href) { throw new NullPointerException(); }
        this.messenger = messenger;
        this.href = href;
    }
    
    /**
     * Constructs a remote reference importer.
     * @param _         corresponding eventual operator
     * @param deferred  {@link Deferred} permission
     * @param messenger network message sender
     * @param here      URL for local vat
     */
    static public Importer
    connect(final Eventual _, final Token deferred,
            final Messenger messenger, final String here) {
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Object
            run(final String href, final String base, final Type type) {
                final String url = null!=base ? URI.resolve(base, href) : href;
                return _.cast(Typedef.raw(type),
                    new Remote(_, deferred, messenger, URI.relate(here, url)));
            }
        }
        return new ImporterX();
    }
    
    /**
     * Constructs a remote reference exporter.
     * @param to    messenger to export for
     * @param next  next exporter to try
     */
    static public Exporter
    export(final Messenger to, final Exporter next) {
        class ExporterX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;
            
            public String
            run(final Object target) {
                final Object handler = target instanceof Proxy
                    ? Proxies.getHandler((Proxy)target) : target;
                if (handler instanceof Remote) {
                    final Remote x = (Remote)handler;
                    if (x.messenger.equals(to)) { return x.href; }
                }
                return next.run(target);
            }
        }
        return new ExporterX();
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
               messenger.equals(((Remote)x).messenger) &&
               _.equals(((Remote)x)._);
    }
    
    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0x4E307E4F; }

    // org.ref_send.promise.Volatile interface

    /**
     * @return <code>this</code>
     */
    public Object
    cast() { return this; }
    
    // java.lang.reflect.InvocationHandler interface

    @Override public Object
    invoke(final Object proxy, final Method method,
           final Object[] arg) throws Exception {
        if (Object.class == method.getDeclaringClass()) {
            if ("equals".equals(method.getName())) {
                return arg[0] instanceof Proxy &&
                    proxy.getClass() == arg[0].getClass() &&
                    equals(Proxies.getHandler((Proxy)arg[0]));
            } else {
                return Reflection.invoke(method, this, arg);
            }
        }
        try {
            return messenger.invoke(href, proxy, method, arg);
        } catch (final Exception e) { throw new Error(e); }
    }
    
    // org.ref_send.promise.eventual.Deferred interface

    protected <R> R
    when(final Class<?> R, final Do<Object,R> observer) {
        return messenger.when(href, this, R, observer);
    }
    
    // org.waterken.remote.Remote interface
    
    /**
     * A {@linkplain #when when block} implementation for a fulfilled reference.
     */
    public <R> R
    fulfill(final Class<?> R, final Do<Object,R> observer) {
        final R r;
        final Do<Object,?> forwarder;
        if (void.class == R || Void.class == R) {
            r = null;
            forwarder = observer;
        } else {
            final Channel<R> x = _.defer();
            r = _.cast(R, x.promise);
            forwarder = new Compose<Object,R>(observer, x.resolver);
        }
        class Fulfill extends Struct implements Task<Void>, Serializable {
            static private final long serialVersionUID = 1L;

            public Void
            run() throws Exception {
                // AUDIT: call to untrusted application code
                forwarder.fulfill(_.cast(
                    Typedef.raw(Compose.parameter(forwarder)), Remote.this));
                // TODO: the logging here could be better
                return null;
            }
        }
        _.run(new Fulfill());
        return r;
    }
}
