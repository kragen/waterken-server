// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.PrintStream;

import org.joe_e.file.Filesystem;
import org.ref_send.log.Event;
import org.ref_send.promise.Receiver;
import org.waterken.db.Database;
import org.waterken.db.DatabaseManager;
import org.waterken.http.Server;
import org.waterken.jos.JODBManager;
import org.waterken.project.Project;
import org.waterken.remote.http.AMP;
import org.waterken.remote.mux.Mux;
import org.waterken.store.n2v.RollingN2V;
import org.waterken.syntax.config.Config;
import org.waterken.thread.Sleep;

/**
 * Server settings.
 */
public final class
Settings {
    private Settings() {}

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
    
    /**
     * file containing the SSL configuration
     */
    static public    final File keys= Filesystem.file(configFolder, "keys.jks");
    
    // remaining configuration is stored in the config folder
    
    /**
     * configuration settings
     */
    static public    final Config config =
        new Config(configFolder, code, "file:///",
                   AMP.connect(new Proxy()),
                   new File(System.getProperty("user.home")));
    static {
        config.override("fileMetadata", new FilesystemClock());
        config.override("stdin", System.in);
        config.override("stdout", System.out);
        config.override("stderr", System.err);
        config.override("os", System.getProperty("os.name"));
        
        final Receiver<Event> log;
        try { log = config.read("log");
        } catch (final Exception e) { throw new Error(e); }
        config.override("dbm", new JODBManager<Server>(
            new RollingN2V(new Sleep()), new Proxy(), log));
    }
    
    /**
     * Connects to a named database.
     * @param path  path to the database
     */
    static public Database<Server>
    db(final String path) throws Exception {
        final DatabaseManager<Server> dbm = config.read("dbm");
        final File root = config.read("vatRootFolder");
        return dbm.connect(Mux.descend(root, path));
    }
    
    /**
     * Prints a summary of the configuration information.
     * @param hostname  configured SSL hostname
     * @param err       output stream
     */
    static protected void
    summarize(final String hostname, final PrintStream err) {
        err.println("config folder: <" + configFolder + ">");
        err.println("hostname: <" + hostname + ">");
    }
}
