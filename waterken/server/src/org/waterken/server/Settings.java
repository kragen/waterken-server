// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.PrintStream;

import org.joe_e.file.Filesystem;
import org.ref_send.log.Event;
import org.ref_send.promise.eventual.Receiver;
import org.waterken.db.Database;
import org.waterken.db.DatabaseManager;
import org.waterken.http.Server;
import org.waterken.jos.JODBManager;
import org.waterken.project.Project;
import org.waterken.store.StoreMaker;
import org.waterken.syntax.config.Config;

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
    
    // remaining configuration is stored in the config folder
    static public    final Config config = new Config(configFolder, code);
    static {
        config.override("tag", new LastModified());
        
        final StoreMaker layout;
        final Receiver<Event> stderr;
        try {
            layout = config.read("storeMaker");
            stderr = config.read("stderr");
        } catch (final Exception e) { throw new Error(e); }
        config.override("vats",
            new JODBManager<Server>(layout, new Proxy(), stderr));
    }
    
    static protected Database<Server>
    vat() throws Exception {
        final DatabaseManager<Server> vats = config.read("vats");
        final File root = config.readType("vatRootFolder", File.class);
        return vats.connect(root);
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
