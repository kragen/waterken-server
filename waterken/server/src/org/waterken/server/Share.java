// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.PrintStream;

import org.joe_e.Token;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.jos.JODB;
import org.waterken.model.Creator;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Transaction;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.remote.http.AMP;
import org.waterken.uri.Hostname;
import org.waterken.uri.URI;

/**
 * Command line program to create a new database.
 */
final class
Share {

    private
    Share() {}
    
    /**
     * URI sub-hierarchy for persistent databases
     */
    static protected final String baseProperty = "waterken.base";
    static protected final String baseDefault = "-/";
    
    /**
     * @param args  command line arguments
     */
    static public void
    main(String[] args) throws Exception {
        
        // initialize the static state
        final File home = new File(
            System.getProperty(JODB.homePathProperty, "")).getCanonicalFile();
        final String base = System.getProperty(baseProperty, baseDefault);
        final String dbPathConfig = System.getProperty(JODB.dbPathProperty);
        final File db = (null != dbPathConfig
            ? new File(dbPathConfig)
        : new File(home, JODB.dbPathDefault)).getCanonicalFile();

        final File keys = new File(home, "keys.jks");

        // extract the arguments
        if (args.length < 2) {
            final PrintStream log = System.err;
            log.println("Creates a new database.");
            log.println("use: java -jar share.jar " +
                "<project-name> <factory-typename> <database-label>");
            System.exit(-1);
            return;
        }
        final String projectValue = args[0];
        final String typename = args[1];
        final String label = 2 < args.length ? args[2] : null;

        // determine the local address
        final String hereValue;
        if (keys.isFile()) {
            final Credentials credentials = SSL.keystore("TLS",keys,"nopass");
            final String host = credentials.getHostname();
            Hostname.vet(host);
            hereValue = "https://" + host + "/" + base;
        } else {
            hereValue = "http://localhost:8080/" + base;
        }
        final Proxy clientValue = new Proxy();
        
        // create the database
        final String r = JODB.connect(db).enter(Model.change,
                                                new Transaction<String>() {
            public String
            run(final Root local) throws Exception {
                final Token deferredValue = new Token();
                final Eventual _Value = new Eventual(deferredValue, null);
                final Root synthetic = new Root() {
                    
                    public String
                    getModelName() { return local.getModelName(); }

                    public Object
                    fetch(final Object otherwise, final String name) {
                        return Root.project.equals(name)
                            ? projectValue
                        : (Remoting.here.equals(name)
                            ? hereValue
                        : (Remoting.client.equals(name)
                            ? clientValue
                        : (Remoting.deferred.equals(name)
                            ? deferredValue
                        : (Remoting._.equals(name)
                            ? _Value
                        : local.fetch(otherwise, name)))));
                    }

                    public void
                    link(final String name,
                         final Object value) { throw new AssertionError(); }

                    public String
                    export(final Object value) {throw new AssertionError(); }

                    public String
                    pipeline(final String m) { throw new AssertionError(); }

                    public String
                    getTransactionTag() { throw new AssertionError(); }
                };
                final Creator creator =
                    (Creator)local.fetch(null, Root.creator);
                final ClassLoader code = creator.load(projectValue);
                final Class<?> factory = code.loadClass(typename);
                return Remote.bind(synthetic, null).
                    run(AMP.publish(synthetic).spawn(label, factory));
            }
        }).cast();
        System.out.println(URI.resolve(hereValue, r));
    }
}
