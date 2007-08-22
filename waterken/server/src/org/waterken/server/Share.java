// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.net.InetAddress;

import org.joe_e.Token;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.jos.JODB;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Transaction;
import org.waterken.remote.Remote;
import org.waterken.remote.Remoting;
import org.waterken.remote.http.AMP;
import org.waterken.remote.mux.Mux;
import org.waterken.uri.URI;

/**
 * 
 */
final class
Share {

    private
    Share() {}
    
    /**
     * @param args
     */
    static public void
    main(String[] args) throws Exception {
        
        // extract the arguments
        final String label = args[0];
        final String typename = args[1];
        final String projectValue = args[2];

        // determine the local address
        final File home = new File("").getAbsoluteFile();
        final File keys = new File(home, "keys.jks");
        final String hereValue;
        if (keys.isFile()) {
            final InetAddress localhost = InetAddress.getLocalHost();
            final String host = localhost.getCanonicalHostName();
            hereValue = (host.equalsIgnoreCase("localhost")
                ? "http://localhost/"
                : "https://" + host + "/") + Mux.dbPathPrefix;
        } else {
            hereValue = "http://localhost:8080/" + Mux.dbPathPrefix;
        }
        final Proxy clientValue = new Proxy();
        
        // create the database
        final String r = JODB.connect(new File(JODB.dbDirName)).
            enter(Model.change, new Transaction<String>() {
                public String
                run(final Root local) throws Exception {
                    final Token deferredValue = new Token();
                    final Eventual _Value = new Eventual(deferredValue, null);
                    final Root synthetic = new Root() {

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
                        store(final String name, final Object value) {
                            throw new IllegalStateException();
                        }
                    };
                    return Remote.bind(synthetic, null).
                        run(AMP.host(synthetic).share(label, typename));
                }
            });
        System.out.println(URI.resolve(hereValue, r));
    }
}
