// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.thread;

import java.io.Serializable;

import org.ref_send.list.List;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;

/**
 * A concurrent loop.
 */
public final class
Concurrent {

    private
    Concurrent() {}

    /**
     * Constructs an instance.
     * @param group thread group
     * @param name  thread name
     */
    static public <T extends Task> Loop<T>
    loop(final ThreadGroup group, final String name) {
        if (null == group) { throw new NullPointerException(); }
        if (null == name) { throw new NullPointerException(); }
        
        // Assume the caller is trusted infrastructure code since it possesses
        // a thread group object.
        class LoopX implements Loop<T>, Serializable {
            static private final long serialVersionUID = 1L;

            private transient List<T> tasks;
            private transient boolean running;

            public synchronized void
            run(final T task) {

                // Enqueue the task.
                if (null == tasks) { tasks = List.list(); }
                tasks.append(task);

                // Start processing tasks.
                if (!running) {
                    new Thread(group, name) {
                        public void
                        run() {
                            // System.err.println("Processing: " + name);
                            while (true) {
                                final T todo;
                                synchronized (LoopX.this) {
                                    if (tasks.isEmpty()) {
                                        running = false;
                                        break;
                                    }
                                    todo = tasks.pop();
                                }
                                try {
                                    todo.run();
                                } catch (final Exception e) {
                                    e.printStackTrace();
                                }
                                yield();
                            }
                            // System.err.println("Idle: " + name);
                        }
                    }.start();
                    running = true;
                }
            }
        }
        return new LoopX();
    }
}
