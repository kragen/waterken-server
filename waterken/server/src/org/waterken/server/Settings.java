// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.PrintStream;
import java.security.SecureRandom;

import org.joe_e.file.Filesystem;
import org.ref_send.promise.eventual.Sink;
import org.waterken.jos.JODBCache;
import org.waterken.net.Execution;
import org.waterken.project.Project;
import org.waterken.remote.http.Browser;
import org.waterken.syntax.config.Config;
import org.waterken.thread.Concurrent;
import org.waterken.vat.Pool;
import org.waterken.vat.Vat;

/**
 * Server settings.
 */
public final class
Settings {

    private
    Settings() {}

    // initialize bootstrap configuration from system properties
    static private   final File configFolder;
    static private   final ClassLoader code;
    static {
        try {
            configFolder = new File(Project.home, System.getProperty(
                "waterken.config", "config")).getCanonicalFile();
            code = Project.connect("dns");
        } catch (final Exception e) { throw new Error(e); }
    }
    static public    final File keys= Filesystem.file(configFolder, "keys.jks");
    static protected final Pool vats = new JODBCache();
    static protected final Execution exe = new Execution() {
        public void
        sleep(final long ms) throws InterruptedException { Thread.sleep(ms); }
        
        public void
        yield() { Thread.yield(); }
    };
    
    // remaining configuration is stored in the config folder
    static public    final Browser browser = Browser.make(
            new Proxy(), new SecureRandom(), code,
            Concurrent.loop(Thread.currentThread().getThreadGroup(), "config"),
            new Sink());
    static public    final Config config =
        new Config(configFolder, code, browser.connect, browser.export);
    static {
        config.override("vats", vats);
        config.override("tag", new LastModified());
    }
    
    /**
     * Gets the root database.
     * @throws Exception    any problem
     */
    static protected Vat
    vat() throws Exception {
        final File id = config.read("vatRootFolder");
        return vats.connect(id);
    }
    
    /**
     * Prints a summary of the configuration information.
     * @param hostname  configured SSL hostname
     * @param err       output stream
     */
    static protected void
    summarize(final String hostname, final PrintStream err) throws Exception {
        err.println("hostname: <" + hostname + ">");
        err.println("config folder: <" + configFolder + ">");
    }
}
