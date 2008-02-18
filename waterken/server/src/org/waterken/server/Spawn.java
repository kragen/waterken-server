// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.PrintStream;

import org.joe_e.Token;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.net.http.HTTPD;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.remote.http.AMP;
import org.waterken.uri.Hostname;
import org.waterken.uri.URI;
import org.waterken.vat.Creator;
import org.waterken.vat.Vat;
import org.waterken.vat.Root;
import org.waterken.vat.Transaction;

/**
 * Command line program to create a new database.
 */
final class
Spawn {

    private
    Spawn() {}
    
    /**
     * @param args  command line arguments
     */
    static public void
    main(String[] args) throws Exception {

        // extract the arguments
        if (args.length < 2) {
            final PrintStream log = System.err;
            log.println("Creates a new persistent object folder.");
            log.println("use: java -jar spawn.jar " +
                "<project-name> <factory-typename> <database-label>");
            System.exit(-1);
            return;
        }
        final String projectValue = args[0];
        final String typename = args[1];
        final String label = 2 < args.length ? args[2] : null;

        // load configured values
        final String dbURIPathPrefix = (String)Config.read("dbURIPathPrefix");

        // determine the local address
        final String hereValue;
        final Credentials credentials = Proxy.init();
        if (null != credentials) {
            final String host = credentials.getHostname();
            Hostname.vet(host);
            final int portN = ((HTTPD)Config.read("https")).port;
            final String port = 443 == portN ? "" : ":" + portN;
            hereValue = "https://" + host + port + "/" + dbURIPathPrefix;
        } else {
            final int portN = ((HTTPD)Config.read("http")).port;
            final String port = 80 == portN ? "" : ":" + portN;
            hereValue = "http://localhost" + port + "/" + dbURIPathPrefix;
        }
        final Proxy clientValue = new Proxy();
        
        // create the database
        final String r=Config.db().enter(Vat.change,new Transaction<String>(){
            public String
            run(final Root local) throws Exception {
                final Token deferredValue = new Token();
                final Eventual _Value = new Eventual(deferredValue, null);
                final Root synthetic = new Root() {
                    
                    public String
                    getVatName() { return local.getVatName(); }

                    public Object
                    fetch(final Object otherwise, final String name) {
                        return Root.project.equals(name)
                            ? projectValue
                        : Remoting.here.equals(name)
                            ? hereValue
                        : Remoting.client.equals(name)
                            ? clientValue
                        : Remoting.deferred.equals(name)
                            ? deferredValue
                        : Remoting._.equals(name)
                            ? _Value
                        : local.fetch(otherwise, name);
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
                final Volatile<?> p = Eventual.promised(
                        AMP.publish(synthetic).spawn(label, factory));
                return Remote.bind(synthetic, null).run(p.cast());
            }
        }).cast();
        System.out.println(URI.resolve(hereValue, r));
    }
}
