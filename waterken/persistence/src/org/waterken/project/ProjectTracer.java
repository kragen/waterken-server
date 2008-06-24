// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.project;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.joe_e.Struct;
import org.joe_e.array.IntArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.reflect.Reflection;
import org.ref_send.log.CallSite;
import org.ref_send.log.Trace;
import org.waterken.vat.Tracer;

/**
 * Produces a stack trace composed of only calls initiated by project code.
 */
public final class
ProjectTracer {

    /**
     * Constructs an instance.
     * @param from  base class loader
     * @param to    last class loader in the chain to consider
     */
    static public Tracer
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
                    final String name = f[i].getMethodName();
                    int line = -1;
                    String source = null;
                    try {
                        final Class<?> type=from.loadClass(f[i].getClassName());
                        if (Proxy.isProxyClass(type)) {
                            top: for (final Class<?> c : type.getInterfaces()) {
                                for (final Method m : Reflection.methods(c)) {
                                    if (m.getName().equals(name)) {
                                        source=path(m.getDeclaringClass(),null);
                                        break top;
                                    }
                                }
                            }
                        } else {
                            line = f[i].getLineNumber();
                            source = path(type, f[i].getFileName());
                        }
                    } catch (final ClassNotFoundException e) {}
                    site[--j] = new CallSite(source, name, line < 0 ? null :
                        PowerlessArray.array(IntArray.array(line)));
                }
                return new Trace(PowerlessArray.array(site));
            }
            
            public Trace
            dummy(final Member lambda) {
                return new Trace(PowerlessArray.array(new CallSite(
                    path(lambda.getDeclaringClass(), null),
                    lambda.getName(), null)));
            }
            
            private String
            path(final Class<?> type, final String filename) {
                final StringBuilder r = new StringBuilder();
                final ClassLoader project = type.getClassLoader();
                if (project instanceof Project) {
                    r.append(((Project)project).name);
                    r.append("/src/");
                } else {
                    r.append("global/src/");
                }
                final String packageName = type.getPackage().getName(); 
                r.append(packageName.replace('.', '/'));
                if (!"".equals(packageName)) { r.append('/'); }
                if (null != filename) {
                    r.append(filename);
                } else {
                    Class<?> top = type;
                    for (Class<?> i=top; null != i; i=top.getEnclosingClass()) {
                        top = i;
                    }
                    r.append(top.getSimpleName());
                    r.append(".java");
                }
                return r.toString();
            }
            
            private boolean
            include(final StackTraceElement f) {
                try {
                    final Class<?> c = from.loadClass(f.getClassName());
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
