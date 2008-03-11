// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Loop;
import org.waterken.cache.Cache;
import org.waterken.thread.Concurrent;
import org.waterken.vat.Service;
import org.waterken.vat.Pool;
import org.waterken.vat.Vat;

/**
 * An open {@link JODB} cache.
 */
public final class
JODBCache extends Struct implements Pool, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs an instance.
     */
    public
    JODBCache() {}
    
    // org.waterken.vat.Pool interface
    
    static private final Cache<File,JODB> live = Cache.make();
    static private final ThreadGroup threads = new ThreadGroup("vat");

    public Vat
    connect(final File id) throws Exception {
        final File f = id.getCanonicalFile();
        if (!f.isDirectory()) { throw new FileNotFoundException(); }
        synchronized (live) {
            JODB r = live.fetch(null, f);
            if (null != r) { return r; }
            final Loop<Service> service = Concurrent.loop(threads, f.getPath());
            r = new JODB(f, service);
            live.put(f, r);
            return r;
        }
    }
}
