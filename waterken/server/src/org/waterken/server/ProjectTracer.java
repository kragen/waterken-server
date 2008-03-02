// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.IntArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.log.CallSite;
import org.ref_send.log.Trace;
import org.waterken.vat.Tracer;

/**
 * Produces a stack trace composed of only calls initiated by project code.
 */
final class
ProjectTracer {

    static protected Tracer
    make(final ClassLoader from, final ClassLoader to) {
        class TracerX extends Struct implements Tracer, Serializable {
            static private final long serialVersionUID = 1L;

            public Trace
            get() {
                final StackTraceElement[] f =
                    Thread.currentThread().getStackTrace();
                int b = f.length;
                while (0 != b && !include(f[b - 1])) { --b; }
                int t = 0;
                if (0 != b) { while (f.length != t && !include(f[t])) { ++t; } }
                final CallSite[] site = new CallSite[b - t];
                for (int i = b, j = site.length; t != i--;) {
                    final int l = f[i].getLineNumber();
                    site[--j] = new CallSite(
                        f[i].getClassName() + '#' + f[i].getMethodName(),
                        l < 0 ? null : PowerlessArray.array(IntArray.array(l)));
                }
                return new Trace(PowerlessArray.array(site));
            }
            
            private boolean
            include(final StackTraceElement f) {
                try {
                    final Class c = from.loadClass(f.getClassName());
                    final ClassLoader cl = c.getClassLoader();
                    for (ClassLoader i = from; null != i; i = i.getParent()) {
                        if (cl == i) { return true; }
                        if (i == to) { return false; }
                    }
                } catch (final ClassNotFoundException e) {}
                return false;
            }
        }
        return new TracerX();
    }
}
