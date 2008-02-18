// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Server;
import org.waterken.id.Exporter;
import org.waterken.id.Importer;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.vat.Model;
import org.waterken.vat.Root;
import org.waterken.vat.Transaction;
import org.web_send.graph.Collision;

/**
 * A transient, homeless client.
 */
public class
Browser extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * eventual operator
     */
    public final Eventual _;
    
    /**
     * reference importer
     */
    public final Importer connect;
    
    /**
     * reference exporter
     */
    public final Exporter export;
    
    /**
     * Constructs an instance.
     * @param _         {@link #_}
     * @param connect   {@link #connect}
     * @param export    {@link #export}
     */
    public @deserializer
    Browser(@name("_") final Eventual _,
            @name("connect") final Importer connect,
            @name("export") final Exporter export) {
        this._ = _;
        this.connect = connect;
        this.export = export;
    }
    
    /**
     * Creates a {@link Browser}.
     * @param client    local HTTP client
     * @param prng      pseudo-random number generator
     * @param code      local class loader
     * @param enqueue   local event loop
     */
    static public Browser
    make(final Server client, final SecureRandom prng,
         final ClassLoader code, final Loop<Task> enqueue) {
        final Token deferred = new Token();
        final Eventual _ = new Eventual(deferred, enqueue);
        final Root local = new Root() {
            private final ArrayList<Binding> bound = new ArrayList<Binding>();
            
            public String
            getModelName() { return null; }

            public Object
            fetch(final Object otherwise, final String name) {
                final String key = name.toLowerCase();
                for (final Binding x : bound) {
                    if (x.key.equals(key)) { return x.value; }
                }
                return otherwise;
            }

            public void
            link(final String name, final Object value) throws Collision {
                final String key = name.toLowerCase();
                for (final Binding x : bound) {
                    if (x.key.equals(key)) { throw new Collision(); }
                }
                bound.add(new Binding(key, value));
            }

            public String
            export(final Object value) { throw new AssertionError(); }

            public String
            pipeline(final String m) { throw new AssertionError(); }

            public String
            getTransactionTag() { throw new AssertionError(); }
        };
        final Model model = new Model((Loop)enqueue) {
            
            private boolean busy = false;
            
            public synchronized <R> Promise<R>
            enter(final boolean extend,
                  final Transaction<R> body) throws Exception {
                if (busy) { throw new Exception(); }
                busy = true;
                try {
                    final R r = body.run(local);
                    busy = false;
                    return ref(r);
                } catch (final Exception e) {
                    busy = false;
                    return new Rejected<R>(e);
                }
            }
        };
        local.link(Root.code, code);
        local.link(Root.destruct, new Runnable() {
            public void
            run() { throw new Error(); }
        });
        local.link(Root.effect, enqueue);
        local.link(Root.enqueue, enqueue);
        local.link(Root.model, model);
        local.link(Root.nothing, null);
        local.link(Root.prng, prng);
        local.link(Remoting._, _);
        local.link(Remoting.client, client);
        local.link(Remoting.deferred, deferred);
        local.link(AMP.outbound, new Outbound());
        return new Browser(_, Remote.use(local), Remote.bind(local, null));
    }

    static private final class
    Binding {
        final String key;
        final Object value;
        
        Binding(final String key, final Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
