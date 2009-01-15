// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.detach;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.joe_e.Immutable;
import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.array.ByteArray;
import org.joe_e.array.ConstArray;
import org.joe_e.file.InvalidFilenameException;
import org.joe_e.reflect.Reflection;
import org.ref_send.list.List;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Log;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;
import org.waterken.db.Creator;
import org.waterken.db.Database;
import org.waterken.db.Effect;
import org.waterken.db.Root;
import org.waterken.db.Transaction;
import org.waterken.http.Server;
import org.waterken.remote.Messenger;
import org.waterken.remote.MessengerSchemeDispatcher;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;
import org.waterken.uri.URI;
import org.web_send.graph.Publisher;
import org.web_send.graph.Spawn;
import org.web_send.graph.Vat;

/**
 * The vat initialization transaction.
 */
public final class
VatInitializer extends Transaction<ByteArray> {

    private final boolean anonymous;
    private final Method build;
    private final ByteArray body;   // JSON serialized arguments
    
    private
    VatInitializer(final boolean anonymous,
                   final Method build, final ByteArray body) {
        super(Transaction.update);
        this.anonymous = anonymous;
        this.build = build;
        this.body = body;
    }
    
    public ByteArray
    run(final Root local) throws Exception {
        final String here = local.fetch(null, Database.here);
        final Receiver<Effect<Server>> effect=local.fetch(null,Database.effect);
        final Log log = local.fetch(null, Database.log);
        final Receiver<?> destruct = local.fetch(null, Database.destruct);
        
        local.link(sessions, new SessionMaker(local));
        final Outbound outbound = new Outbound();
        local.link(VatInitializer.outbound, outbound);
        local.link(Database.wake, new Wake());
        final List<Task<?>> tasks = List.list();
        local.link(VatInitializer.tasks, tasks);
        final Token deferred = new Token();
        local.link(VatInitializer.deferred, deferred);
        final Eventual _= new Eventual(deferred,enqueue(effect,tasks),here,log);
        local.link(VatInitializer._, _);
        final Messenger messenger = new MessengerSchemeDispatcher(local);
        local.link(VatInitializer.messenger, messenger);
        final Messenger http = new HTTP(_, effect, detach(outbound), local);
        local.link(MessengerSchemeDispatcher.prefix + "http", http);
        local.link(MessengerSchemeDispatcher.prefix + "https", http);
        final Publisher publisher = publish(local);
        final Vat vat = new Vat(
            destruct, spawn(publisher), anonymous ? null : publisher
        );
        final ConstArray<Type> signature =
            ConstArray.array(build.getGenericParameterTypes());
        ConstArray<Type> parameters = signature;
        if (parameters.length() != 0) {     // pop the eventual operator
            parameters = parameters.without(0);
        }
        if (parameters.length() != 0) {     // pop the vat permissions
            parameters = parameters.without(0);
        }
        final Exports exports = new Exports(local);
        final ConstArray<?> optional = new JSONDeserializer().run(
            URI.resolve(here, ".."), exports.connect(),
            parameters, exports.code, body.asInputStream());
        final Object[] argv = new Object[signature.length()];
        if (0 < argv.length) { argv[0] = _; }
        if (1 < argv.length) { argv[1] = vat; }
        for (int i = 2; i < argv.length; ++i) {
            argv[i] = optional.get(i - 2);
        }
        final Object value = Reflection.invoke(build, null, argv);
        return new JSONSerializer().run(exports.send(URI.resolve(here, "..")),
                                        ConstArray.array(value));
    }

    /**
     * Constructs a reference exporter.
     * @param mother    local vat root
     */
    static private Publisher
    publish(final Root mother) {
        class PublisherX extends Publisher implements Serializable {
            static private final long serialVersionUID = 1L;

            public void
            bind(final String name, final Object value) {
                vet(name);
                mother.link(name, value);
            }

            public @SuppressWarnings("unchecked") <R> R
            spawn(final String name,
                  final Class<?> maker, final Object... argv) {
                final Method make = Exports.dispatch(maker, "make");
                final Class<?> R = make.getReturnType();
                try {
                    if (null != name) { vet(name); }
                    final Exports exports = new Exports(mother);
                    final ByteArray body = new JSONSerializer().run(
                            exports.export(), ConstArray.array(argv));  
                    final String project = mother.fetch(null, Database.project);
                    final Creator creator = mother.fetch(null,Database.creator);
                    final ByteArray response = creator.run(project, name,
                        new VatInitializer(null == name, make, body)).cast();
                    return (R)new JSONDeserializer().run(
                        exports.getHere(), exports.connect(),
                        ConstArray.array(make.getGenericReturnType()),
                        exports.code, response.asInputStream()).get(0);
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
    
    static public ByteArray
    create(final Database<Server> parent, final String here,
           final String project, final Class<?> maker,
           final String label) throws Exception {
        return parent.enter(new Transaction<ByteArray>(Transaction.update) {
            public ByteArray
            run(final Root local) throws Exception {
                final Root synthetic = new Root() {

                    public @SuppressWarnings("unchecked") Object
                    fetch(final Object otherwise, final String name) {
                        return Database.project.equals(name)
                            ? project
                        : Database.here.equals(name)
                            ? here
                        : local.fetch(otherwise, name);
                    }

                    public void
                    link(final String name,
                         final Object value) { throw new AssertionError(); }

                    public String
                    export(final Object value,
                           final boolean isWeak) { throw new AssertionError(); }
                };

                final Object value = publish(synthetic).spawn(label, maker);
                final Exports exports = new Exports(local);
                return new JSONSerializer().run(exports.send(null),
                                                ConstArray.array(value));
            }
        }).cast();
    }
    
    static private Spawn
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
                    local.fetch(null, Database.effect);
                effect.run(runTask());
            }
            final Outbound outbound= local.fetch(null, VatInitializer.outbound);
            for (final Pipeline x : outbound.getPending()) { x.resend(); }
            return null;
        }
    }
    
    static private Effect<Server>
    runTask() {
        return new Effect<Server>() {
            public void
            run(final Database<Server> vat) throws Exception {
                vat.enter(new Transaction<Immutable>(Transaction.update) {
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
    
    static protected final String outbound = ".outbound";
    static private   final String tasks = ".tasks";
    static protected final String _ = "._";
    static protected final String deferred = ".deferred";
    static protected final String messenger = ".messenger";

    /**
     * key bound to the session maker in all vats
     */
    static protected final String sessions = "sessions";
}
