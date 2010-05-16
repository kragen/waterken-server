// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.db.Scheduler;

/**
 * Puts an event on a specified event loop after a timeout.
 */
public final class
LoopScheduler<T extends Promise<?>> implements Scheduler<T> {

    /**
     * shared timeout thread
     */
    static private final ScheduledExecutorService timeouts =
        Executors.newSingleThreadScheduledExecutor();
    
    /**
     * event loop for executing tasks
     */
    public final Receiver<T> loop;

    private
    LoopScheduler(final Receiver<T> loop) {
        this.loop = loop;
    }
    
    /**
     * Constructs an instance.
     * @param loop  {@link #loop}
     */
    static public <T extends Promise<?>> Scheduler<T>
    make(final Receiver<T> loop) { return new LoopScheduler<T>(loop); }
    
    public void
    apply(final long timeout, final T task) {
        timeouts.schedule(new Runnable() {
            public void
            run() { loop.apply(task); }
        }, timeout, TimeUnit.MILLISECONDS);
    }
}
