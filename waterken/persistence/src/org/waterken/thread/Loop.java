// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.thread;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;

/**
 * A concurrent loop.
 */
public final class
Loop<T extends Promise<?>> {
    
    /**
     * enqueue tasks to be executed as-soon-as-possible
     */
    public final Receiver<T> foreground;
    
    /**
     * enqueue tasks to be executed when otherwise idle
     */
    public final Receiver<Promise<?>> background;
    
    private
    Loop(final Receiver<T> foreground, final Receiver<Promise<?>> background) {
        this.foreground = foreground;
        this.background = background;
    }
    
    /**
     * global thread pool
     */
    static public final ExecutorService pool = Executors.newCachedThreadPool();

    /**
     * Constructs an instance.
     * @param name  loop name
     */
    static public <T extends Promise<?>> Loop<T>
    make(final String name) {
        if (null == name) { throw new NullPointerException(); }

        final Object lock = new Object();
        final LinkedList<T> foreground = new LinkedList<T>();        
        final LinkedList<Promise<?>> background = new LinkedList<Promise<?>>();
        final boolean[] running = new boolean[] { false };
        final Runnable runner = new Runnable() {
            public void
            run() {
                // System.out.println(name + ": processing...");
            	final String originalName = Thread.currentThread().getName();
                try {
                	Thread.currentThread().setName(name);
                    while (true) {
                        final Promise<?> todo;
                        synchronized (lock) {
                            if (foreground.isEmpty()) {
                                if (background.isEmpty()) {
                                    running[0] = false;
                                    break;
                                } else {
                                    todo = background.removeFirst();
                                }
                            } else {
                                todo = foreground.removeFirst();
                            }
                        }
                        try {
                            todo.call();
                        } catch (final Exception e) {
                            System.err.println(name + ":");
                            e.printStackTrace(System.err);
                        }
                    }
                } catch (final Throwable e) {
                    System.err.println(name + ":");
                    e.printStackTrace(System.err);
                } finally {
                	Thread.currentThread().setName(originalName);
                }
                // System.out.println(name + ": idling...");
            }
        };
        class Enqueue<X> implements Receiver<X> {
            private final LinkedList<X> tasks;
            
            Enqueue(final LinkedList<X> tasks) {
                this.tasks = tasks;
            }
            
            public void
            apply(final X task) {
                if (null == task) { throw new NullPointerException(); }
                
                synchronized (lock) {
                    tasks.add(task);
                    if (!running[0]) {
                        running[0] = true;
                        pool.submit(runner);
                    }
                }
            }
        }
        return new Loop<T>(new Enqueue<T>(foreground),
                           new Enqueue<Promise<?>>(background));
    }
}
