// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.Principal;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.joe_e.file.Filesystem;
import org.waterken.net.Locator;
import org.waterken.uri.Authority;
import org.waterken.uri.Location;

/**
 * 
 */
final class
SSL {

    private
    SSL() {}

    /**
     * Opens an SSL keystore.
     * @param protocol      standard name of the protocol
     * @param file          key file
     * @param passphrase    key file passphrase
     */
    static Credentials
    keystore(final String protocol, final File file, final String passphrase) {
        class KeystoreX implements Credentials, Serializable {
            static private final long serialVersionUID = 1L;

            private transient SSLContext context;

            public SSLContext
            getContext() throws IOException, GeneralSecurityException {
                if (null == context) {

                    // Load the key store.
                    final KeyStore keys =
                        KeyStore.getInstance(KeyStore.getDefaultType());
                    final InputStream in = Filesystem.read(file);
                    keys.load(in, passphrase.toCharArray());
                    in.close();

                    // Construct the key managers.
                    final KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(keys, passphrase.toCharArray());
                    final KeyManager[] km = kmf.getKeyManagers();

                    final TrustManagerFactory tmf =
                        TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(keys);
                    final TrustManager[] tm = tmf.getTrustManagers();

                    // Build the SSL context.
                    final SSLContext c = SSLContext.getInstance(protocol);
                    c.init(km, tm, null);
                    context = c;
                }
                return context;
            }
        }
        return new KeystoreX();
    }
    
    static Locator
    client(final int standardPort, final Credentials credentials) {
        class ClientX implements Locator, Serializable {
            static private final long serialVersionUID = 1L;

            private transient SSLSocketFactory factory;

            public String
            canonicalize(final String authority) {
                final String location = Authority.location(authority);
                final String host = Location.host(location);
                final int port = Location.port(standardPort, location);
                return host.toLowerCase()+(standardPort==port ? "" : ":"+port);
            }
            
            public Socket
            locate(final String authority,
                   final SocketAddress x) throws IOException {
                final String location = Authority.location(authority);
                final String host = Location.host(location);
                final int port = Location.port(standardPort, location);
                try {
                    if (null == factory) {
                        factory = credentials.getContext().getSocketFactory();
                    }
                    IOException reason = new ConnectException();
                    for (final InetAddress a : InetAddress.getAllByName(host)) {
                        try {
                            final SSLSocket r =
                                (SSLSocket)factory.createSocket(a, port);

                            // Restrict the acceptable ciphersuites.
                            r.setEnabledCipherSuites(new String[] {
                                "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                                "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
                                "TLS_RSA_WITH_AES_128_CBC_SHA",
                                "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
                                "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
                                "SSL_RSA_WITH_3DES_EDE_CBC_SHA"
                            });

                            // Verify peer name and requested hostname match.
                            final Principal peer =
                                r.getSession().getPeerPrincipal();
                            final String dn = peer.getName();
                            final int cn = dn.indexOf("CN=");
                            if (-1 == cn) { throw new IOException(); }
                            final int start_cn = cn + "CN=".length();
                            final int end_cn = dn.indexOf(',', start_cn);
                            final String name = -1 == end_cn
                                ? dn.substring(start_cn)
                                : dn.substring(start_cn, end_cn);
                            if (!host.equalsIgnoreCase(name)) {
                                throw new IOException();
                            }
                            return r;
                        } catch (final IOException e) {
                            reason = e; // Keep trying.
                        }
                    }
                    throw reason;
                } catch (final GeneralSecurityException e) {
                    throw (IOException)new IOException().initCause(e);
                }
            }
        }
        return new ClientX();
    }
}
