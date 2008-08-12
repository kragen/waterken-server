// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.ref;
import static org.ref_send.scope.Scope.Empty;

import java.io.Serializable;
import java.security.SecureRandom;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.log.Anchor;
import org.ref_send.log.Turn;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;
import org.ref_send.scope.Scope;
import org.waterken.http.Server;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.vat.Root;
import org.waterken.vat.Service;
import org.waterken.vat.Transaction;
import org.waterken.vat.Vat;
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
     * @param log       optional event log
     */
    static public Browser
    make(final Server client, final SecureRandom prng, final ClassLoader code,
         final Loop<Task> enqueue, final Log log) {
        final long[] turns = { 0L };
        final Root local = new Root() {
            private Scope bound = Empty.make();
            private long events = 0L;
            
            public String
            getVatName() { return null; }

            public Anchor
            anchor() { return new Anchor(new Turn(null, turns[0]), events++); }

			public @SuppressWarnings("unchecked") Object
            fetch(final Object otherwise, final String name) {
			    final int i = bound.meta.find(name.toLowerCase());
			    return -1 != i ? bound.values.get(i) : otherwise;
            }

            public void
            link(final String name, final Object value) throws Collision {
                bound = bound.with(name.toLowerCase(), value);
            }

            public String
            export(final Object value) { throw new AssertionError(); }

            public String
            pipeline(final String m, final long w, final long i) {
                throw new AssertionError();
            }

            public String
            getTransactionTag() { throw new AssertionError(); }
        };
        final Vat vat = new Vat(reuse(enqueue)) {
            
            private boolean busy = false;

            public String
            getProject() { return null; }
            
            public synchronized <R> Promise<R>
            enter(final boolean extend,
                  final Transaction<R> body) throws Exception {
                if (busy) { throw new Exception(); }
                busy = true;
                Promise<R> rp;
                try {
                    final R r = body.run(local);
                    rp = ref(r);
                } catch (final Exception e) {
                    rp = new Rejected<R>(e);
                }
                ++turns[0];
                busy = false;
                return rp;
            }
        };
        final Token deferred = new Token();
        final Eventual _ = new Eventual(deferred, enqueue, log);
        local.link(Root.code, code);
        local.link(Root.destruct, new Receiver<Object>() {
            public void
            run(final Object ignored) { throw new Error(); }
        });
        local.link(Root.effect, enqueue);
        local.link(Root.enqueue, enqueue);
        local.link(Root.vat, vat);
        local.link(Root.nothing, null);
        local.link(Root.prng, prng);
        local.link(Remoting._, _);
        local.link(Remoting.client, client);
        local.link(Remoting.deferred, deferred);
        local.link(AMP.outbound, new Outbound());
        return new Browser(_, Remote.use(local), Remote.bind(local, null));
    }
    
	static private @SuppressWarnings("unchecked") Loop<Service>
    reuse(final Loop<Task> loop) { return (Loop)loop; }
}
