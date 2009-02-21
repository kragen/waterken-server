// Copyright 2005-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.FileFilter;
import java.net.BindException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

import org.joe_e.Immutable;
import org.joe_e.array.ByteArray;
import org.ref_send.promise.Receiver;
import org.waterken.db.DatabaseManager;
import org.waterken.db.Root;
import org.waterken.db.Transaction;
import org.waterken.http.Server;
import org.waterken.net.TCPDaemon;
import org.waterken.udp.UDPDaemon;

/**
 * Starts the server.
 */
final class
Serve {

    private
    Serve() {}
    
    /**
     * @param args  command line arguments
     */
    static public void
    main(String[] args) throws Exception {
        if (args.length == 0) {
            args = Settings.keys.isFile()
                ? new String[] { "http", "https" }
            : new String[] { "http" };
        }
        try {
            start(args);
            return;
        } catch (final Exception e) {
            e.printStackTrace(System.err);
        } catch (final Error e) {
            final Throwable cause = e.getCause();
            final Throwable reason = null != cause ? cause : e;
            reason.printStackTrace(System.err);
        }
        System.err.println();
        System.err.println("!!! server exiting due to configuration error !!!");
        System.exit(-1);
    }
    
    static private void
    start(final String... services) throws Exception {
        final Credentials credentials = Proxy.credentials;
        final String hostname =
            null != credentials ? credentials.getHostname() : "localhost";

        // summarize the configuration information
        Settings.summarize(hostname, System.out);

        // start the network services
        final Receiver<ByteArray> updateDNS_= Settings.config.read("updateDNS");
        for (final String service : services) {
        	try {
	            final Object config = Settings.config.read(service);
	            final Runnable task;
	            if (config instanceof TCPDaemon) {
	                final TCPDaemon daemon = (TCPDaemon)config;
	                final ServerSocket listen = daemon.SSL
	                    ? credentials.getContext().getServerSocketFactory().
	                        createServerSocket(daemon.port, daemon.backlog)
	                : new ServerSocket(daemon.port, daemon.backlog,
	                				   Loopback.addr);
	                task = new TCP(daemon, daemon.SSL ? hostname : "localhost",
	                               listen, updateDNS_);
	            } else if (config instanceof UDPDaemon) {
	                final UDPDaemon daemon = (UDPDaemon)config;
	                task = new UDP(daemon, new DatagramSocket(daemon.port));
	            } else if (config instanceof Runnable) {
	                task = (Runnable)config;
	            } else {
	            	throw new Exception("Unrecognized service: " + service);
	            }
	
	            // run the corresponding daemon
	            new Thread(task, service).start();
	        } catch (final BindException e) {
	        	System.err.println("Cannot use configured port for: "+service);
	        	System.err.println("Try configuring a different port number " +
	        	    "using the corresponding file in the config/ folder.");
	        	throw e;
	        }
        }

        // ping all the persistent vats to restart any pending tasks
        System.out.println(Thread.currentThread() + ": restarting all vats...");
        final DatabaseManager<Server> vats = Settings.config.read("dbs");
        final File root = Settings.config.read("vatRootFolder");
        ping(vats, root);
        System.out.println(Thread.currentThread() + ": all vats restarted");
    }
    
    static private void
    ping(final DatabaseManager<Server> vats, final File dir) {
        dir.listFiles(new FileFilter() {
            public boolean
            accept(final File child) {
                if (child.isDirectory() && !child.getName().startsWith(".")) {
                    ping(vats, child);
                }
                return false;
            }
        });
        try {
            vats.connect(dir).enter(Transaction.query,
                                    new Transaction<Immutable>() {
                public Immutable
                run(final Root local) { return null; }
            }).cast();
        } catch (final Exception e) { e.printStackTrace(); }
    }}
