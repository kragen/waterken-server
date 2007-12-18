// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.security.SecureRandom;
import java.util.Enumeration;

import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.file.Filesystem;
import org.ref_send.var.Setter;
import org.ref_send.var.Variable;
import org.waterken.dns.Resource;
import org.waterken.dns.editor.ResourceGuard;
import org.waterken.net.Daemon;
import org.waterken.remote.http.Browser;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.thread.Concurrent;

/**
 * A TCP daemon.
 */
final class
TCP implements Runnable {
    
    private final PrintStream err;
    private final ThreadGroup threads;
    private final String protocol;
    private final Daemon daemon;
    private final int soTimeout;
    private final ServerSocket port;
    private final File ip;
    
    private       long count = 0;
    
    TCP(final PrintStream err, final ThreadGroup threads,  
        final String protocol, final Daemon daemon,
        final int soTimeout, final ServerSocket port,
        final File ip) {
        
        this.err = err;
        this.threads = threads;
        this.protocol = protocol;
        this.daemon = daemon;
        this.soTimeout = soTimeout;
        this.port = port;
        this.ip = ip;
    }

    public void
    run() {
        err.println(protocol + ": " + "running at " +
                    port.getLocalSocketAddress() + " ...");
        
        final Setter<Resource> updater_;
        if (null != ip && ip.isFile()) {
            Setter<Resource> tmp_;
            try {
                tmp_ = update(ip);
                port.setSoTimeout(60 * 1000);
            } catch (final Exception e) {
                tmp_ = null;
                err.println(protocol + ": " + e);
            }
            updater_ = tmp_;
        } else {
            updater_ = null;
        }
        InetAddress address = null;
        boolean recheck = null != updater_;
        while (true) {
            final Socket socket;
            try {
                if (recheck) {
                    final InetAddress a = dynip();
                    if (!a.equals(address)) {
                        err.println(
                            "Updating DNS to: " + a.getHostAddress() + "...");
                        updater_.set(new Resource(
                            Resource.A, Resource.IN, ResourceGuard.minTTL,
                            ByteArray.array(a.getAddress())));
                        address = a;
                    }
                    recheck = false;
                }

                socket = port.accept();
            } catch (final SocketTimeoutException e) {
                recheck = true;
                continue;
            } catch (final Exception e) {
                // Something strange happened.
                e.printStackTrace();
                continue;
            }
            final String name = protocol + "-" + count++;
            new Thread(threads, new Runnable() {
                public void
                run() {
                    try {
                        err.println(name + ": processing...");
                        socket.setSoTimeout(soTimeout);
                        daemon.accept(socket).run();
                    } catch (final Throwable e) {
                        err.println(name + ": " + e);
                    } finally {
                        try { socket.close(); } catch (final Exception e) {}
                    }
                }
            }, name).start();
        }
    }
    
    static private InetAddress
    dynip() throws SocketException {
        InetAddress r = Loopback.addr;
        for (final Enumeration<NetworkInterface> j =
                                NetworkInterface.getNetworkInterfaces();
                                                     j.hasMoreElements();) {
            for (final Enumeration<InetAddress> k =
                                j.nextElement().getInetAddresses();
                                                     k.hasMoreElements();) {
                final InetAddress a = k.nextElement();
                if (a instanceof Inet4Address && !a.isLoopbackAddress()) {
                    if (!a.isSiteLocalAddress()) { return a; }
                    r = a;
                }
            }
        }
        return r;
    }
    
    @SuppressWarnings("unchecked") static private Setter<Resource>
    update(final File ip) throws Exception {
        final ClassLoader code = GenKey.class.getClassLoader();
        final Browser browser = Browser.make(
            new Proxy(), new SecureRandom(), code,
            Concurrent.loop(Thread.currentThread().getThreadGroup(), "dynip"));
        final InputStream in = Filesystem.read(ip);
        final Type type = Variable.class;
        final Variable<Resource> r = (Variable)new JSONDeserializer().
            run("",browser.connect,code,in,PowerlessArray.array(type)).get(0);
        in.close();
        return browser._._(r.setter);
    }
}
