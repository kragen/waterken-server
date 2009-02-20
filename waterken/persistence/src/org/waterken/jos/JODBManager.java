// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;

import org.ref_send.log.Event;
import org.ref_send.promise.Receiver;
import org.waterken.cache.Cache;
import org.waterken.db.DatabaseManager;
import org.waterken.db.Service;
import org.waterken.store.StoreMaker;
import org.waterken.thread.Concurrent;
import org.waterken.thread.Sleep;

/**
 * A cache of live vats.
 */
public final class
JODBManager<S> implements DatabaseManager<S> {
    
    private final ThreadGroup group = new ThreadGroup("db");
    private final Cache<File,JODB<S>> live = Cache.make();
    private final Sleep sleep = new Sleep();

    private final StoreMaker layout;
    private final S session;
    private final Receiver<Event> stderr;
    
    /**
     * Constructs an instance.
     * @param session   session state for all vats
     * @param stderr    standard error output for all vats
     * @param layout    store maker
     */
    public
    JODBManager(final StoreMaker layout, final S session,
                final Receiver<Event> stderr) {
        this.layout = layout;
        this.session = session;
        this.stderr = stderr;
    }

    public JODB<S>
    connect(final File id) throws Exception {
        final File dir = id.getCanonicalFile();
        synchronized (live) {
            JODB<S> r = live.fetch(null, dir);
            if (null != r) { return r; }
            final Receiver<Service> service =
                Concurrent.make(group, dir.getPath());            
            r = new JODB<S>(session, service, stderr,
                            layout.run(sleep, dir.getParentFile(), dir));
            live.put(dir, r);
            return r;
        }
    }
}
