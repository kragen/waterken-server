// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.ImmutableArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Log;
import org.ref_send.promise.NotAMaker;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.db.Creator;
import org.waterken.db.Database;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.Transaction;
import org.waterken.http.Server;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.json.JSONDeserializer;

/**
 * The vat initialization transaction.
 */
public final class
VatInitializer extends Struct implements Transaction<PowerlessArray<String>> {

    private final Method make;      // maker method
    private final String base;      // base URL for JSON serialization
    private final ByteArray body;   // JSON serialized arguments
    
    protected
    VatInitializer(final Method make, final String base, final ByteArray body) {
        this.make = make;
        this.base = base;
        this.body = body;
    }
    
    public PowerlessArray<String>
    apply(final Root root) throws Exception {
        final Log log_ = root.fetch(null, Database.log);
        final Receiver<Effect<Server>> effect= root.fetch(null,Database.effect);
        
        final List<Promise<?>> tasks = List.list();
        final Receiver<?> destruct = root.fetch(null, Database.destruct);
        final Outbound outbound = new Outbound();
        final SessionMaker sessions = new SessionMaker(root, log_);
        final HTTP.Exports exports = HTTP.make(
                decouple(Eventual.ref(enqueue(effect,tasks))), root,
                log_, destruct, Eventual.ref(outbound), sessions);
        final String mid = exports.getHere() + "#make";
        log_.got(mid, null, make);
        root.assign(VatInitializer.tasks, tasks);
        root.assign(VatInitializer.outbound, outbound);
        root.assign(VatInitializer.exports, exports);
        root.assign(VatInitializer.sessions, sessions);
        root.assign(Database.wake, wake(tasks, outbound));
        Type[] paramv = make.getGenericParameterTypes();
        final Object[] argv = new Object[paramv.length];
        int nextArg = 0;
        if (0 != paramv.length && Eventual.class == paramv[0]) {
            final int paramc = paramv.length - 1;
            System.arraycopy(paramv, 1, paramv = new Type[paramc], 0, paramc);
            argv[nextArg++] = exports._;
        }
        final ConstArray<?> optional = new JSONDeserializer().deserializeTuple(
            body.asInputStream(), exports.connect(), base, exports.code,paramv);
        for (final Object arg : optional) { argv[nextArg++] = arg; }
        final Object top = Reflection.invoke(make, null, argv);
        root.assign(Database.top, top);
        final Exporter export =
            HTTP.changeBase(exports.getHere(), exports.export(), base);
        return PowerlessArray.array(mid, export.apply(top).href,
                                    export.apply(destruct).href);
    }
    
    static public String
    create(final Database<Server> parent, final String project,
           final String base, final String label,
           final Class<?> maker, final ByteArray body) throws Exception {
        final Method make = NotAMaker.dispatch(maker);
        return parent.enter(Database.update,
                            new Transaction<PowerlessArray<String>>() {
            public PowerlessArray<String>
            apply(final Root local) throws Exception {
                final Creator creator = local.fetch(null, Database.creator);
                return creator.apply(project, base, label,
                                     new VatInitializer(make,null,body)).call();
            }
        }).call().get(1);
    }
    
    static private Receiver<Promise<?>>
    decouple(final Promise<Receiver<Promise<?>>> enqueue) {
        class Decoupler extends Struct
                        implements Receiver<Promise<?>>, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            apply(final Promise<?> task) {
                Eventual.near(enqueue).apply(task);
            }
        }
        return new Decoupler();
    }
    
    static private Receiver<Promise<?>>
    enqueue(final Receiver<Effect<Server>> effect,final List<Promise<?>> tasks){
        class Enqueue extends Struct
                      implements Receiver<Promise<?>>, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            apply(final Promise<?> task) {
                if (tasks.isEmpty()) { effect.apply(runTask()); }
                tasks.append(task);
            }
        }
        return new Enqueue();
    }
    
    static private Promise<ImmutableArray<Effect<Server>>>
    wake(final List<Promise<?>> tasks, final Outbound outbound) {
        class Wake extends Struct
                   implements Promise<ImmutableArray<Effect<Server>>>,
                              Serializable {
            static private final long serialVersionUID = 1L;
            
            public ImmutableArray<Effect<Server>>
            call() {
                final ImmutableArray.Builder<Effect<Server>> r =
                    ImmutableArray.builder();
                if (!tasks.isEmpty()) { r.append(runTask()); }
                for (final Pipeline pending : outbound.getPending()) {
                    for (final Effect<Server> effect : pending.resend()) {
                        r.append(effect);
                    }
                }
                return r.snapshot();
            }
        }
        return new Wake();
    }
    
    static protected Effect<Server>
    runTask() {
        return new Effect<Server>() {
            public void
            apply(final Database<Server> vat) throws Exception {
                vat.enter(Database.update, new Transaction<Immutable>() {
                    public Immutable
                    apply(final Root local) throws Exception {
                        final List<Promise<?>> tasks =
                            local.fetch(null, VatInitializer.tasks);
                        final Promise<?> task = tasks.pop();
                        if (!tasks.isEmpty()) {
                            final Receiver<Effect<Server>> effect =
                                local.fetch(null, Database.effect);
                            effect.apply(runTask());
                        }
                        task.call();
                        return new Token();
                    }
                }).call();
            }
        };
    }

    /**
     * key bound to the session maker in all vats
     */
    static private   final String sessions = "sessions";
    
    static protected final String outbound = ".outbound";
    static private   final String tasks = ".tasks";
    static public    final String exports = ".exports";
}
