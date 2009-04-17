// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.joe_e.array.PowerlessArray;
import org.joe_e.file.Filesystem;
import org.waterken.base32.Base32;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.net.Locator;
import org.waterken.net.http.ClientSide;
import org.waterken.uri.Authority;
import org.waterken.uri.Header;
import org.waterken.uri.Location;

/**
 * SSL implementation
 */
final class
SSL {

    private
    SSL() {}
    
    static Locator
    client(final int standardPort,
           final Credentials credentials, final PrintStream log) {
        class ClientX implements Locator, Serializable {
            static private final long serialVersionUID = 1L;

            private transient SSLSocketFactory factory;

            public String
            canonicalize(final String authority) {
                final String location = Authority.location(authority);
                final String hostname = Location.hostname(location);
                final int port = Location.port(standardPort, location);
                return Header.toLowerCase(hostname) +
                       (standardPort == port ? "" : ":" + port);
            }
            
            public Socket
            locate(final String authority,
                   final SocketAddress x) throws IOException{
                if (null == factory) {
                    if (null != credentials) {
                        try {
                            factory=credentials.getContext().getSocketFactory();
                        } catch (final GeneralSecurityException e) {
                            throw (IOException)new IOException().initCause(e);  
                        }
                    } else {
                        factory=(SSLSocketFactory)SSLSocketFactory.getDefault();
                    }
                }
                
                final String location = Authority.location(authority);
                
                log.println(Thread.currentThread().getName() +
                            ": connecting to <" + location + ">...");
                
                final String hostname = Location.hostname(location);
                final int port = Location.port(standardPort, location);
                final InetAddress[] addrs = InetAddress.getAllByName(hostname);
                final int[] pending = { addrs.length };
                final Socket[] winner = { null };
                class Racer extends Thread {
                    private final Connect connect;
                    
                    Racer(final Connect connect) {
                        this.connect = connect;
                    }
                    
                    public void
                    run() {
                        Socket authenticated = null;
                        try {
                            final Socket r = connect.run();
                            try {
                                authenticate(hostname, (SSLSocket)r);
                                authenticated = r;
                            } catch (final Exception e) {
                                r.close();
                                throw e;
                            }
                        } catch (final Exception e) {
                        } finally {
                            synchronized (pending) {
                                if (null != authenticated) {
                                    if (null == winner[0]) {
                                        winner[0] = authenticated;
                                        pending.notify();
                                    } else {
                                        try { authenticated.close(); }
                                        catch (final IOException e) {}
                                    }
                                } else if (0 == --pending[0]) {
                                    pending.notify();
                                }
                            }
                        }
                    }
                }
                synchronized (pending) {
                    if (Boolean.parseBoolean(System.getProperty("proxySet"))) {
                        ++pending[0];
                        new Racer(proxy(factory, hostname, port)).start();
                    }
                    for (final InetAddress addr : addrs) {
                        new Racer(direct(factory, addr, port)).start();
                    }
                    if (0 != pending[0]) {
                        try {pending.wait();} catch (InterruptedException e) {}
                    }
                    if (null != winner[0]) { return winner[0]; }
                }
                throw new ConnectException();
            }
        }
        return new ClientX();
    }
    
    interface Connect {
        Socket run() throws Exception;
    }
    
    static private Connect
    proxy(final SSLSocketFactory factory,final String hostname, final int port){
        return new Connect() {
            public Socket
            run() throws Exception {
                final Socket proxy = new Socket(
                    System.getProperty("proxyHost"),
                    Integer.parseInt(System.getProperty("proxyPort")));
                try {
                    final OutputStream out = proxy.getOutputStream();
                    ClientSide.send(new Request(
                        "HTTP/1.0", "CONNECT", hostname + ":" + port,
                        PowerlessArray.array(new Header[0])), null, out);
                    out.flush();
                    final boolean[] connected = { false };
                    ClientSide.receive("CONNECT", proxy.getInputStream(),
                                       new Client() {
                        public void
                        receive(final Response head,
                            final InputStream body) throws Exception {
                            if ("200".equals(head.status)) {
                                connected[0] = true;
                            }
                        }
                    });
                    if (!connected[0]) { throw new ConnectException(); }
                    return factory.createSocket(proxy, hostname, port, true);
                } catch (final Exception e) {
                    proxy.close();
                    throw e;
                }
            }
        };
    }
    
    static private Connect
    direct(final SSLSocketFactory factory,
           final InetAddress addr, final int port) {
        return new Connect() {
            public Socket
            run() throws Exception { return factory.createSocket(addr, port); }
        };
    }
    
    static private void
    authenticate(final String hostname,
                 final SSLSocket socket) throws Exception {

        // restrict the acceptable ciphersuites
        socket.setEnabledCipherSuites(new String[] {
            "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
            "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
            "TLS_RSA_WITH_AES_128_CBC_SHA",
            "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA",
            "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
            "SSL_RSA_WITH_3DES_EDE_CBC_SHA"
        });

        // verify peer name and requested hostname match
        final SSLSession session = socket.getSession();
        final String cn = CN(session.getPeerPrincipal().getName());
        if (matches(hostname, cn)) { return; }
        final X509Certificate ee =
            (X509Certificate)session.getPeerCertificates()[0];
        final Collection<List<?>> alts = ee.getSubjectAlternativeNames();
        if (null == alts) { throw new SSLPeerUnverifiedException("CN"); }
        for (final List<?> alt : alts) {
            final Object name = alt.get(1);
            if (name instanceof String &&
                matches(hostname, (String)name)) { return; }
        }
        throw new SSLPeerUnverifiedException("CN");
    }
    
    static private boolean
    matches(final String hostname, final String pattern) {
        return pattern.startsWith("*")
            ? hostname.regionMatches(true,
                hostname.length() - pattern.length() + 1,
                pattern, 1, pattern.length() - 1)
        : Header.equivalent(hostname, pattern);
    }

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

            private transient String hostname;
            private transient SSLContext context;

            public String
            getHostname()  throws IOException, GeneralSecurityException {
                init();
                if (null == hostname) { throw new NullPointerException(); }
                return hostname;
            }

            public SSLContext
            getContext() throws IOException, GeneralSecurityException {
                init();
                return context;
            }
            
            private void
            init() throws IOException, GeneralSecurityException {
                if (null == context) {

                    // load the key store
                    final KeyStore keys =
                        KeyStore.getInstance(KeyStore.getDefaultType());
                    final InputStream in = Filesystem.read(file);
                    keys.load(in, passphrase.toCharArray());
                    in.close();

                    // extract the keys and certs
                    final KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(keys, passphrase.toCharArray());
                    final KeyManager[] kms = kmf.getKeyManagers();

                    final TrustManagerFactory tmf =
                        TrustManagerFactory.getInstance(
                            TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(keys);
                    final TrustManager[] tms = tmf.getTrustManagers();

                    // Augment the underlying PKI with y-property semantics.
                    for (int i = 0; i != tms.length; ++i) {
                        if (tms[i] instanceof X509TrustManager) {
                            tms[i] = y((X509TrustManager)tms[i]);
                            break;
                        }
                    }

                    // build the SSL context
                    final SSLContext c = SSLContext.getInstance(protocol);
                    c.init(kms, tms, null);
                    context = c;
                    
                    // determine the local hostname
                    for (final KeyManager km : kms) {
                        if (km instanceof X509KeyManager) {
                            final X509KeyManager x509 = (X509KeyManager)km;
                            final X509Certificate[] chain =
                                x509.getCertificateChain("mykey");
                            if (null != chain && chain.length > 0) {
                                hostname = CN(chain[0].
                                        getSubjectX500Principal().getName());
                                if (null != hostname) { return; }
                            }
                        }
                    }
                }
            }
        }
        return new KeystoreX();
    }
    
    static private final CertificateException notY = new CertificateException();
    
    /**
     * Constructs a trust manager that implements the y-property.
     * @param pki   default key verification algorithm
     */
    static private X509TrustManager
    y(final X509TrustManager pki) {
        return new X509TrustManager() {

            public X509Certificate[]
            getAcceptedIssuers() { return pki.getAcceptedIssuers(); }

            public void
            checkClientTrusted(final X509Certificate[] chain,
                    final String authType) throws CertificateException {
                if (!checkY(chain, authType)) {
                    pki.checkClientTrusted(chain, authType);
                }
            }

            public void
            checkServerTrusted(final X509Certificate[] chain,
                    final String authType) throws CertificateException {
                if (!checkY(chain, authType)) {
                    pki.checkServerTrusted(chain, authType);
                }
            }

            private boolean
            checkY(final X509Certificate[] chain,
                   final String authType) throws CertificateException {
                // determine whether or not the cert uses the y-property
                // a cert using the y-property MUST ONLY specify a CN property
                final X509Certificate cert = chain[0];
                final String dn = cert.getSubjectX500Principal().getName();
                if (!dn.startsWith("CN=")) { return false; }
                final String cn = dn.substring("CN=".length());
                final String hostname = Header.toLowerCase(cn);
                if (!hostname.startsWith("y-")) { return false; }
                if (!hostname.endsWith(".yurl.net")) { return false; }
                final String fingerprint = hostname.substring("y-".length(),
                        hostname.length() - ".yurl.net".length());

                // TODO: figure out how this API works with longer cert chains
                if (1 != chain.length) { throw notY; }
                
                // certificate is not valid for any other name
                if (null != cert.getSubjectAlternativeNames()) { throw notY; }
                
                // the caller's role is unspecified, so check the basic
                // certificate validity properties just in case
                try {
                    cert.verify(cert.getPublicKey());
                } catch (final Exception e) { throw notY; }
                cert.checkValidity();

                // check that the fingerprint matches the given public key
                final byte[] guid;
                switch (fingerprint.length()) {
                case 16:    // 80 bit hash
                    guid = new byte[10];
                    break;
                case 23:    // 112 bit hash
                    guid = new byte[14];
                    break;
                case 26:    // 128 bit hash
                    guid = new byte[16];
                    break;
                default:    // not a hash
                    throw notY;
                }
                final MessageDigest alg;
                final String algname = cert.getSigAlgName();
                if ("SHA1withRSA".equals(algname)) {
                    try {
                        alg = MessageDigest.getInstance("SHA-1");
                    } catch (final Exception e) { throw notY; }
                } else {
                    throw notY;
                }
                System.arraycopy(alg.digest(cert.getPublicKey().getEncoded()),
                                 0, guid, 0, guid.length);
                if (!fingerprint.equals(Base32.encode(guid))) { throw notY; }
                return true;
            }
        };
    }    
    
    /**
     * Extracts the CN from a peer.
     * @param dn    distinguished name
     * @return CN value, or <code>null</code> if not specified
     */
    static private String
    CN(final String dn) {
        if (!dn.startsWith("CN=")) { return null; }
        final int startCN = "CN=".length();
        final int endCN = dn.indexOf(',', startCN);
        return -1==endCN ? dn.substring(startCN) : dn.substring(startCN, endCN);
    }
}
