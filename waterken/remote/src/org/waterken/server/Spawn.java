// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import org.waterken.net.http.HTTPD;
import org.waterken.project.Project;
import org.waterken.remote.http.VatInitializer;
import org.waterken.uri.Hostname;

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
    main(final String[] args) throws Exception {

        // extract the arguments
        if (args.length < 2) {
            System.err.println("Creates a new persistent object folder.");
            System.err.println("use: java -jar spawn.jar " +
                "<project-name> <maker-typename> <database-label>");
            System.exit(-1);
            return;
        }
        final String project = args[0];
        final String typename = args[1];
        final String label = 2 < args.length ? args[2] : null;
        final Object[] argv;
        if (3 < args.length) {
            argv = new Object[args.length - 3];
            for (int i = 3; i != args.length; ++i) {
                try {
                    argv[i - 3] = Integer.parseInt(args[i]);
                } catch (final Exception e) {
                    argv[i - 3] = args[i];
                }
            }
        } else {
            argv = new Object[0];
        }

        // load configured values
        final String vatURIPathPrefix= Settings.config.read("vatURIPathPrefix");

        // determine the local address
        final String here;
        final Credentials credentials = Proxy.credentials;
        if (null != credentials) {
            final String host = credentials.getHostname();
            Hostname.vet(host);
            final HTTPD https = Settings.config.read("https");
            final int portN = https.port;
            final String port = 443 == portN ? "" : ":" + portN;
            here = "https://" + host + port + "/" + vatURIPathPrefix;
        } else {
        	final HTTPD http = Settings.config.read("http");
            final int portN = http.port;
            final String port = 80 == portN ? "" : ":" + portN;
            here = "http://localhost" + port + "/" + vatURIPathPrefix;
        }
        
        // create the database
        final ClassLoader code = Project.connect(project);
        final Class<?> maker = code.loadClass(typename);
        final String r = VatInitializer.create(Settings.db(""), project,
                                               here, label, maker, argv);
        System.out.println(r);
    }
}
