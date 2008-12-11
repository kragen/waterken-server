// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import static org.ref_send.promise.Fulfilled.ref;
import static org.web_send.Failure.maxEntitySize;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.charset.URLEncoding;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.deserializer;
import org.ref_send.list.List;
import org.ref_send.log.Event;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Do;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Factory;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Task;
import org.waterken.http.MediaType;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.io.limited.Limited;
import org.waterken.io.snapshot.Snapshot;
import org.waterken.remote.mux.Remoting;
import org.waterken.trace.EventSender;
import org.waterken.trace.Tracer;
import org.waterken.trace.TurnCounter;
import org.waterken.uri.Path;
import org.waterken.uri.URI;
import org.waterken.vat.Creator;
import org.waterken.vat.Effect;
import org.waterken.vat.Root;
import org.waterken.vat.Service;
import org.waterken.vat.Transaction;
import org.waterken.vat.Vat;
import org.web_send.Failure;
import org.web_send.graph.Framework;
import org.web_send.graph.Publisher;
import org.web_send.graph.Spawn;

/**
 * HTTP web-AMP implementation
 */
public final class
AMP extends Struct implements Remoting, Powerless, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * MIME Media-Type for marshalled arguments
     */
    static protected final MediaType mime =
    	new MediaType("application", "jsonrequest");
    
    /**
     * Constructs an instance.
     */
    public @deserializer
    AMP() {}
    
    // org.waterken.remote.Remoting interface

    public Server
    remote(final Server bootstrap, final String scheme, final Vat vat) {
        return new Server() {
            public void
            serve(final String resource,
                  final Volatile<Request> requestor,
                  final Do<Response,?> respond) throws Exception {

                // check for web browser bootstrap request
                final String query = URI.query(null, resource);
                if (null == query) {
                    bootstrap.serve("file:///site/" +
                            URLEncoding.encode(vat.getProject()) + "/" +
                            URLEncoding.encode(Path.name(URI.path(resource))),
                        requestor, respond);
                    return;
                }

                final Request buffered; {
                    Request q = requestor.cast();
                    if (null != q.body) {
                        final Integer length = q.getContentLength();
                        if (null != length && length > maxEntitySize) {
                        	throw Failure.tooBig();
                        }
                        if (!q.expectContinue(respond)) { return; }
                        q = new Request(q.version, q.method, q.URL, q.header,
                            Snapshot.snapshot(null != length ? length : 1024,
                                Limited.limit(maxEntitySize, q.body)));
                    } else {
                        if (!q.expectContinue(respond)) { return; }
                    }
                    buffered = q;
                }
                final Promise<Response> respondor =
                        vat.enter(new Transaction<Response>(
                                          "GET".equals(buffered.method) ||
                                          "HEAD".equals(buffered.method) ||
                                          "OPTIONS".equals(buffered.method) ||
                                          "TRACE".equals(buffered.method)) {
                    public Response
                    run(final Root local) throws Exception {
                        final Response[] response = { null };
                        new Callee(local).serve(resource, ref(buffered),
                                                new Do<Response,Void>() {
                            public Void
                            fulfill(Response r) throws Exception {
                                if (null != r.body &&
                                    !(r.body instanceof Snapshot)) {
                                    r= new Response(r.version,r.status,r.phrase,
                                        r.header, Snapshot.snapshot(
                                            r.getContentLength(), r.body));
                                }
                                response[0] = r;
                                return null;
                            }
                        });
                        return response[0];
                    }
                });
                final Response response;
                try {
                    response = respondor.cast();
                } catch (final Exception e) {
                    respond.reject(e);
                    return;
                }
                respond.fulfill(response);
            }
        };
    }

    // org.waterken.remote.http.AMP interface
    
    static protected Spawn
    spawn(final Publisher publisher) {
        class SpawnX extends Spawn implements Serializable {
            static private final long serialVersionUID = 1L;
            
			public <R> R
            run(final Class<?> maker, final Object... argv) {
			    return publisher.spawn(null, maker, argv);
			}
        }
        return new SpawnX();
    }

    /**
     * Constructs a reference exporter.
     * @param mother    local vat root
     */
    static public Publisher
    publish(final Root mother) {
        class PublisherX extends Publisher implements Serializable {
            static private final long serialVersionUID = 1L;

            public void
            bind(final String name, final Object value) {
                vet(name);
                mother.link(name, value);
            }

			public <R> R
            spawn(final String name,
                  final Class<?> builder, final Object... argv) {
			    final Method build = Exports.dispatch(builder, "build");
			    final Class<?> R = build.getReturnType();
                try {
                    if (null != name) { vet(name); }
                    final Tracer tracer = mother.fetch(null, AMP.tracer);
                    final String project = mother.fetch(null, Vat.project);
                    final Creator creator = mother.fetch(null, Vat.creator);
                    return creator.run(project, name, new Transaction<R>() {
                        public R
                        run(final Root local) throws Exception {
                            local.link(AMP.tracer, tracer);
                            final List<Task> tasks = List.list();
                            local.link(AMP.tasks, tasks);
                            final Token deferred = new Token();
                            local.link(Vat.deferred, deferred);
                            final String here = local.fetch(null, Vat.here);
                            final TurnCounter counter = TurnCounter.make(here);
                            local.link(Vat.flip, counter.flip);
                            final ClassLoader code= local.fetch(null, Vat.code);
                            final Vat vat = local.fetch(null, Vat.vat);
                            final Loop<Effect> effect =
                                local.fetch(null, Vat.effect);
                            final Eventual _ = new Eventual(deferred,
                                EventSender.makeLoop(counter.mark, code, tracer,
                                    dst, enqueue(vat, effect, tasks)),
                                EventSender.makeLog(counter.mark, code, tracer,
                                    dst, name));
                            local.link(Vat._, _);
                            local.link(outbound, new Outbound());
                            local.link(Vat.wake, new Wake());
                            final Receiver<?> destruct =
                                local.fetch(null, Vat.destruct);
                            final Publisher publisher = publish(local);
                            final Framework framework = new Framework(
                                _,
                                EventSender.makeDestructor(counter.mark,
                                                           code, tracer,
                                                           dst, destruct),
                                AMP.spawn(publisher),
                                null != name ? publisher : null
                            );
                            // export the maker and the framework,
                            // then send an eventual invocation.
                        }
                    }).cast();
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
    
    static private Factory<Receiver<Event>>
    dst(final Loop<Effect> effect, final Receiver<Event> send) {
        class Send extends Struct implements Receiver<Event>, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            run(final Event value) {
                effect.run(new Effect() {
                    public void
                    run() { send.run(value); }
                });
            }
        }
        class Dst extends Factory<Receiver<Event>> implements Serializable {
            static private final long serialVersionUID = 1L;

            public Receiver<Event>
            run() { return null != send ? new Send() : null; }
        }
        return new Dst();
    }
    
    static private Loop<Task>
    enqueue(final Vat vat, final Loop<Effect> effect, final List<Task> tasks) {
        class Enqueue extends Struct implements Loop<Task>, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            run(final Task task) {
                if (tasks.isEmpty()) {
                    effect.run(runTask(vat));
                }
                tasks.append(task);
            }
        }
        return new Enqueue();
    }

    static private final class
    Wake extends Struct implements Transaction<Void>, Powerless, Serializable {
        static private final long serialVersionUID = 1L;

        public Void
        run(final Root local) throws Exception {
            final List<Task> tasks = local.fetch(null, AMP.tasks);
            if (!tasks.isEmpty()) {
                final Loop<Effect> effect = local.fetch(null, Vat.effect);
                final Vat vat = local.fetch(null, Vat.vat);
                effect.run(runTask(vat));
            }
            final Outbound outbound = local.fetch(null, AMP.outbound);
            for (final Outbound.Entry x : outbound.getPending()) {
                x.msgs.resend();
            }
            return null;
        }
    }
    
    static private Effect
    runTask(final Vat vat) {
        return new Effect() {
            public void
            run() {
                vat.service.run(new Service() {
                    public void
                    run() throws Exception {
                        vat.enter(Vat.change, new Transaction<Void>() {
                            public Void
                            run(final Root local) throws Exception {
                                final List<Task> tasks =
                                    local.fetch(null, AMP.tasks);
                                final Task task = tasks.pop();
                                if (!tasks.isEmpty()) {
                                    final Loop<Effect> effect =
                                        local.fetch(null, Vat.effect);
                                    effect.run(runTask(vat));
                                }
                                task.run();
                                return null;
                            }
                        });
                    }
                });
            }
        };
    }
    
    static private final String outbound = ".outbound";
    static private final String tasks = ".tasks";
    static private final String tracer = ".tracer";
}
