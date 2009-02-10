// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.list.List;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Log;
import org.ref_send.promise.Receiver;
import org.ref_send.promise.Task;
import org.waterken.db.Creator;
import org.waterken.db.Database;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.Transaction;
import org.waterken.http.Server;
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
    run(final Root local) throws Exception {
        final Log log = local.fetch(null, Database.log);
        final Receiver<Effect<Server>> effect=local.fetch(null,Database.effect);
        
        final List<Task<?>> tasks = List.list();
        final Outbound outbound = new Outbound();
        final HTTP.Exports exports = HTTP.make(enqueue(effect,tasks),local, log,
                                               Eventual.detach(outbound));
        log.got(exports.getHere() + "#make", make);
        local.link(VatInitializer.tasks, tasks);
        local.link(VatInitializer.outbound, outbound);
        local.link(VatInitializer.exports, exports);
        local.link(VatInitializer.sessions, new SessionMaker(local));
        local.link(Database.wake, wake(tasks, outbound, effect));
        final ConstArray<Type> signature =
            ConstArray.array(make.getGenericParameterTypes());
        ConstArray<Type> parameters = signature;
        if (parameters.length() != 0) {     // pop the eventual operator
            parameters = parameters.without(0);
        }
        final ConstArray<?> optional = new JSONDeserializer().run(base,
            exports.connect(), parameters, exports.getCodebase(),
            body.asInputStream());
        final Object[] argv = new Object[signature.length()];
        if (argv.length != 0) { argv[0] = exports._; }
        for (int i = 0; i != optional.length(); ++i) {
            argv[i + 1] = optional.get(i);
        }
        final Object value = Reflection.invoke(make, null, argv);
        return PowerlessArray.array(HTTP.changeBase(
                exports.getHere(), exports.export(), base).run(value));
    }
    
    static public String
    create(final Database<Server> parent, final String project,
           final String base, final String label,
           final Class<?> maker) throws Exception {
        final Method make = HTTP.dispatch(maker, "make");
        final ByteArray body = ByteArray.array((byte)'[', (byte)']');
        return parent.enter(Transaction.update,
                            new Transaction<PowerlessArray<String>>() {
            public PowerlessArray<String>
            run(final Root local) throws Exception {
                final Creator creator = local.fetch(null, Database.creator);
                return creator.run(project, base, label,
                                   new VatInitializer(make, null, body)).cast();
            }
        }).cast().get(0);
    }
    
    static private Receiver<Task<?>>
    enqueue(final Receiver<Effect<Server>> effect, final List<Task<?>> tasks) {
        class Enqueue extends Struct implements Receiver<Task<?>>, Serializable{
            static private final long serialVersionUID = 1L;

            public void
            run(final Task<?> task) {
                if (tasks.isEmpty()) {
                    effect.run(runTask());
                }
                tasks.append(task);
            }
        }
        return new Enqueue();
    }
    
    static private Task<?>
    wake(final List<Task<?>> tasks, final Outbound outbound,
         final Receiver<Effect<Server>> effect) {
        class Wake extends Struct implements Task<Void>, Serializable {
            static private final long serialVersionUID = 1L;
            
            public Void
            run() throws Exception {
                if (!tasks.isEmpty()) {
                    effect.run(runTask());
                }
                for (final Pipeline x : outbound.getPending()) { x.resend(); }
                return null;
            }
        }
        return new Wake();
    }
    
    static private Effect<Server>
    runTask() {
        return new Effect<Server>() {
            public void
            run(final Database<Server> vat) throws Exception {
                vat.enter(Transaction.update, new Transaction<Immutable>() {
                    public Immutable
                    run(final Root local) throws Exception {
                        final List<Task<?>> tasks =
                            local.fetch(null, VatInitializer.tasks);
                        final Task<?> task = tasks.pop();
                        if (!tasks.isEmpty()) {
                            final Receiver<Effect<Server>> effect =
                                local.fetch(null, Database.effect);
                            effect.run(runTask());
                        }
                        task.run();
                        return null;
                    }
                });
            }
        };
    }

    /**
     * key bound to the session maker in all vats
     */
    static private   final String sessions = "sessions";
    
    static protected final String outbound = ".outbound";
    static private   final String tasks = ".tasks";
    static protected final String exports = ".exports";
}
