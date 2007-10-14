// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.security.SecureRandom;
import java.util.ArrayList;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Server;
import org.waterken.id.Exporter;
import org.waterken.id.Importer;
import org.waterken.id.exports.Exports;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Transaction;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;

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
    @SuppressWarnings("unchecked") static public Browser
    make(final Server client,
         final SecureRandom prng,
         final ClassLoader code,
         final Loop<Task> enqueue) {
        final Token deferred = new Token();
        final Eventual _ = new Eventual(deferred, enqueue);
        final Root local = new Root() {
            private final ArrayList<Binding> bound = new ArrayList<Binding>();

            public Object
            fetch(final Object otherwise, final String name) {
                final String key = name.toLowerCase();
                for (final Binding x : bound) {
                    if (x.key.equals(key)) { return x.value; }
                }
                return otherwise;
            }

            public void
            store(final String name, final Object value) {
                final String key = name.toLowerCase();
                for (final Binding x : bound) {
                    if (x.key.equals(key)) {
                        x.value = value;
                        return;
                    }
                }
                bound.add(new Binding(key, value));
            }
        };
        final Model model = new Model((Loop)enqueue) {
            public <R> R
            enter(final boolean extend,
                  final Transaction<R> body) throws Exception, Error {
                return body.run(local);
            }
        };
        local.store(Root.code, code);
        local.store(Root.destruct, new Runnable() {
            public void
            run() { throw new Error(); }
        });
        local.store(Root.effect, enqueue);
        local.store(Root.enqueue, enqueue);
        local.store(Root.model, model);
        local.store(Root.nothing, null);
        local.store(Root.prng, prng);
        local.store(Remoting._, _);
        local.store(Remoting.client, client);
        local.store(Remoting.deferred, deferred);
        local.store(AMP.outbound, new Outbound());
        Exports.initialize(local);
        return new Browser(_,
                           Remote.use(local),
                           Remote.bind(local, null));
    }

    private static final class
    Binding {
        final String key;
              Object value;
        
        Binding(final String key, final Object value) {
            this.key = key;
            this.value = value;
        }
    }
}
