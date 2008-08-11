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
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Deferred;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.type.Typedef;
import org.waterken.remote.http.HTTP;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.uri.URI;
import org.waterken.vat.Root;

/**
 * A remote reference.
 */
public final class
Remote extends Deferred<Object> implements Promise<Object> {
    static private final long serialVersionUID = 1L;

    /**
     * local address space
     */
    private final Root local;

    /**
     * reference relative URL
     */
    private final String URL;

    private
    Remote(final Root local, final String URL) {
        super((Eventual)local.fetch(null, Remoting._),
              (Token)local.fetch(null, Remoting.deferred));
        if (null == URL) { throw new NullPointerException(); }
        this.local = local;
        this.URL = URL;
    }
    
    /**
     * Creates a remote reference.
     * @param local local address space
     * @param URL   reference URL
     * @param type  referent type
     */
    static public Object
    _(final Root local, final String URL, final Class<?> type) {
        final String here = local.fetch(null, Root.here);
        final String target = null == here ? URL : URI.relate(here, URL);
        final Remote rp = new Remote(local, target);
        return rp._.cast(type, rp);
    }
    
    /**
     * Constructs an importer.
     * @param local local address space
     */
    static public Importer
    use(final Root local) {
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;

            public Object
            run(final String href, final String base, final Type type) {
                final String URL = null != base ? URI.resolve(base,href) : href;
                return _(local, URL, Typedef.raw(type));
            }
        }
        return new ImporterX();
    }
    
    /**
     * Constructs an exporter.
     * @param local local address space
     * @param next  next module to try
     */
    static public Exporter
    bind(final Root local, final Exporter next) {
        class ExporterX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public String
            run(final Object object) {
                final Object handler = object instanceof Proxy
                    ? Proxies.getHandler((Proxy)object) : object;
                if (handler instanceof Remote) {
                    final Remote x = (Remote)handler;
                    if ((Token)local.fetch(null, Remoting.deferred) ==
                        (Token)x.local.fetch(null, Remoting.deferred)) {
                        return x.URL;
                    }
                }
                return next.run(object);
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
        return x instanceof Remote && _.equals(((Remote)x)._) &&
               URL.equals(((Remote)x).URL) &&
               local.equals(((Remote)x).local);
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
            final String here = local.fetch(null, Root.here);
            final String target = null == here ? URL : URI.resolve(here, URL);
            return message(target).invoke(target, proxy, method, arg);
        } catch (final Exception e) { throw new Error(e); }
    }
    
    // org.ref_send.promise.eventual.Deferred interface

    protected <R> R
    when(final Class<?> R, final Do<Object,R> observer) {
        final String here = local.fetch(null, Root.here);
        final String target = null == here ? URL : URI.resolve(here, URL);
        return message(target).when(target, R, observer);
    }

    private Messenger
    message(final String target) {
        final String scheme = URI.scheme("", target);
        if ("https".equals(scheme)) { return new HTTP("https", 443, local); }
        if ("http".equals(scheme)) { return new HTTP("http", 80, local); }
        final Rejected<Object> answer =
        	new Rejected<Object>(new UnknownScheme(scheme));
        return new Messenger() {
            
            public <R> R
            when(final String URL, final Class<?> R,
                 final Do<Object,R> observer) { return when(answer, observer); }
            
            /**
             * A trick to resolve a dispatch ambiguity in javac.
             */
            private <P,R> R
            when(final Promise<P> value, final Do<P,R> observer) {
                final Eventual _ = local.fetch(null, Remoting._);
                return _.when(value, observer);
            }
            
            public Object
            invoke(final String URL, final Object proxy, 
                   final Method method, final Object... arg) {
                try {
                    return answer.invoke(proxy, method, arg);
                } catch (final Exception e) { throw new Error(e); }
            }
        };
    }
}
