// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.list.List;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.Server;
import org.waterken.remote.Messenger;
import org.waterken.remote.MessengerSchemeDispatcher;
import org.waterken.remote.Remote;
import org.waterken.syntax.Importer;
import org.waterken.vat.Creator;
import org.waterken.vat.Effect;
import org.waterken.vat.Root;
import org.waterken.vat.Transaction;
import org.waterken.vat.Vat;
import org.web_send.graph.Framework;
import org.web_send.graph.Publisher;
import org.web_send.graph.Spawn;

/**
 * The vat initialization transaction.
 */
/* package */ final class
VatInitializer<R> extends Transaction<PowerlessArray<String>> {

    private final boolean anonymous;
    private final Method build;
    
    private
    VatInitializer(final boolean anonymous, final Method build) {
        super(Transaction.update);
        this.anonymous = anonymous;
        this.build = build;
    }
    
    public PowerlessArray<String>
    run(final Root local) throws Exception {
        final String here = local.fetch(null, Vat.here);
        final Receiver<Effect<Server>> effect = local.fetch(null, Vat.effect);
        final Log log = local.fetch(null, Vat.log);
        final Receiver<?> destruct = local.fetch(null, Vat.destruct);
        
        local.link(outbound, new Outbound());
        local.link(Vat.wake, new Wake());
        final List<Task<?>> tasks = List.list();
        local.link(VatInitializer.tasks, tasks);
        final Token deferred = new Token();
        final Eventual _= new Eventual(deferred,enqueue(effect,tasks),here,log);
        final Publisher publisher = publish(local, _, deferred,
                                    new MessengerSchemeDispatcher(local));
        final Framework framework = new Framework(
            _, destruct, spawn(publisher), anonymous ? null : publisher
        );
        final Channel<R> r = _.defer();
        final Builder<R> init = new Builder<R>(build, framework, r.resolver);
        return PowerlessArray.array(
            here + Exports.href(local.export(init, false), false),
            here + Exports.href(local.export(r.promise, false), true)
        );
    }

    /**
     * Constructs a reference exporter.
     * @param mother    local vat root
     */
    static public Publisher
    publish(final Root mother, final Eventual _,
            final Token deferred, final Messenger messenger) {
        class PublisherX extends Publisher implements Serializable {
            static private final long serialVersionUID = 1L;

            public void
            bind(final String name, final Object value) {
                vet(name);
                mother.link(name, value);
            }

            public @SuppressWarnings("unchecked") <R> R
            spawn(final String name,
                  final Class<?> builder, final Object... argv) {
                final Method build = Exports.dispatch(builder, "build");
                final Class<?> R = build.getReturnType();
                try {
                    if (null != name) { vet(name); }
                    final String project = mother.fetch(null, Vat.project);
                    final Creator creator = mother.fetch(null, Vat.creator);
                    final PowerlessArray<String> keys = creator.run(project,
                        name, new VatInitializer<R>(null==name, build)).cast();
                    final String here = mother.fetch(null, Vat.here);
                    final Importer connect = 
                        Remote.connect(_, deferred, messenger, here);
                    final Receiver<ConstArray<?>> init =
                        (Receiver)connect.run(keys.get(0),null, Receiver.class);
                    init.run(ConstArray.array(argv));
                    return (R)connect.run(keys.get(1), null, R);
                } catch (final Exception e) {
                    return new Rejected<R>(e)._(R);
                }
            }
            
            private void
            vet(final String name) throws InvalidFilenameException {
                if (name.startsWith(".")){throw new InvalidFilenameException();}
                for (int i = name.length(); i-- != 0;) {
                    if (disallowed.indexOf(name.charAt(i)) != -1) {
                        throw new InvalidFilenameException();
                    }
                }
            }
        }
        return new PublisherX();
    }
    
    static protected Spawn
    spawn(final Publisher publisher) {
        class SpawnX extends Spawn implements Serializable {
            static private final long serialVersionUID = 1L;
            
            public <R> R
            run(final Class<?> builder, final Object... argv) {
                return publisher.spawn(null, builder, argv);
            }
        }
        return new SpawnX();
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

    static private final class
    Wake extends Transaction<Immutable> implements Serializable {
        static private final long serialVersionUID = 1L;

        Wake() { super(Transaction.query); }
        
        public Immutable
        run(final Root local) throws Exception {
            final List<Task<?>> tasks = local.fetch(null, VatInitializer.tasks);
            if (!tasks.isEmpty()) {
                final Receiver<Effect<Server>> effect =
                    local.fetch(null, Vat.effect);
                effect.run(runTask());
            }
            final Outbound outbound= local.fetch(null, VatInitializer.outbound);
            for (final Outbound.Entry x : outbound.getPending()) {
                x.msgs.resend();
            }
            return null;
        }
    }
    
    static private Effect<Server>
    runTask() {
        return new Effect<Server>() {
            public void
            run(final Vat<Server> vat) throws Exception {
                vat.enter(new Transaction<Immutable>(Transaction.update) {
                    public Immutable
                    run(final Root local) throws Exception {
                        final List<Task<?>> tasks =
                            local.fetch(null, VatInitializer.tasks);
                        final Task<?> task = tasks.pop();
                        if (!tasks.isEmpty()) {
                            final Receiver<Effect<Server>> effect =
                                local.fetch(null, Vat.effect);
                            effect.run(runTask());
                        }
                        task.run();
                        return null;
                    }
                });
            }
        };
    }
    
    static private final String outbound = ".outbound";
    static private final String tasks = ".tasks";
}
