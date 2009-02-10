// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;

import org.joe_e.array.ByteArray;
import org.ref_send.promise.Receiver;
import org.waterken.dns.Resource;
import org.waterken.net.TCPDaemon;
import org.waterken.thread.Yield;

/**
 * A TCP daemon.
 */
final class
TCP implements Runnable {
    
    private final String serviceName;
    private final PrintStream err;
    private final TCPDaemon daemon;
    private final String hostname;
    private final ServerSocket port;
    private final Receiver<Resource> updateDNS;
    
    private       long connectionCount = 0;
    private       InetAddress lastKnownAddress = null;
    
    TCP(final String serviceName, final PrintStream err,  
        final TCPDaemon daemon, final String hostname,
        final ServerSocket port, final Receiver<Resource> updateDNS) {
        this.serviceName = serviceName;
        this.err = err;
        this.daemon = daemon;
        this.hostname = hostname;
        this.port = port;
        this.updateDNS = updateDNS;
    }

    public void
    run() {
        err.println(serviceName + ": " + "running at " +
                    port.getLocalSocketAddress() + " ...");
        
        final ThreadGroup threads = new ThreadGroup(serviceName);
        if (null != updateDNS) { updateHostAddress(); }
        while (true) {
            final Socket socket;
            try {
                socket = port.accept();
            } catch (final SocketTimeoutException e) {
                if (null != updateDNS) { updateHostAddress(); }
                continue;
            } catch (final Exception e) {
                // something strange happened
                e.printStackTrace();
                continue;
            }
            final String name = serviceName + "-" + connectionCount++;
            new Thread(threads, new Runnable() {
                public void
                run() {
                    try {
                        err.println(name + ": processing...");
                        daemon.accept(hostname, socket, new Yield()).run();
                    } catch (final SocketTimeoutException e) {
                    	// normal end to a TCP connection
                    } catch (final Throwable e) {
                    	e.printStackTrace(err);
                    } finally {
                        try { socket.close(); } catch (final Exception e) {}
                    }
                    err.println(name + ": done");
                }
            }, name).start();
        }
    }
    
    private void
    updateHostAddress() {
        try {
            final InetAddress a = dynip();
            if (!a.equals(lastKnownAddress)) {
                err.println(
                    "Updating DNS to: " + a.getHostAddress() + "...");
                updateDNS.run(new Resource(
                    Resource.A, Resource.IN, 60,
                    ByteArray.array(a.getAddress())));
                lastKnownAddress = a;
            }
        } catch (final Exception e) {
            e.printStackTrace();
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
}
