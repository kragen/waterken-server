// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace.project;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;
import java.net.URLClassLoader;

import org.joe_e.Struct;
import org.joe_e.array.IntArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.log.CallSite;
import org.ref_send.log.Trace;
import org.waterken.trace.Tracer;

/**
 * Produces a stack trace composed of only calls initiated by project code.
 */
public final class
ProjectTracer {

    /**
     * Constructs an instance.
     * @param code      project class loader
     * @param levels    number of relative path segments to include in paths
     */
    static public Tracer
    make(final ClassLoader code, final int levels) {
        if (0 > levels) { throw new RuntimeException(); }
        
        class TracerX extends Struct implements Tracer, Serializable {
            static private final long serialVersionUID = 1L;
            
            public String
            readException(final Throwable e) { return e.getMessage(); }

            public Trace
            traceException(final Throwable e) {return trace(e.getStackTrace());}
            
            public Trace
            traceMember(final Member lambda) {
                return trace(new StackTraceElement(
                    lambda.getDeclaringClass().getName(), lambda.getName(),
                    null, -1));
            }

            public Trace
            traceHere() {return trace(Thread.currentThread().getStackTrace());}
            
            private Trace
            trace(final StackTraceElement... frames) {
                final PowerlessArray.Builder<CallSite> sites =
                    PowerlessArray.builder(frames.length);
                for (final StackTraceElement frame : frames) {
                    String project = null;
                    try {
                        final Class<?> c = code.loadClass(frame.getClassName());
                        final ClassLoader cl = c.getClassLoader();
                        if (!Proxy.isProxyClass(c) &&
                                cl instanceof URLClassLoader) {
                            final String uri =
                                ((URLClassLoader)cl).getURLs()[0].toString();
                            if (uri.endsWith("/")) {
                                int start = uri.length() - 1;
                                for (int i = levels; 0 != i--;) {
                                    start = uri.lastIndexOf('/', start - 1);
                                }
                                project = uri.substring(start + 1);
                            }
                        }
                    } catch (final ClassNotFoundException e) {}
                    if (null == project) {
                        if (0 == sites.length()) { continue; }
                        break;
                    }
                    
                    final String enclosing; {
                        final String name = frame.getClassName();
                        final int end = name.indexOf('$');
                        enclosing= -1 == end ? name : name.substring(0, end);
                    }
                    final int line = frame.getLineNumber();
                    sites.append(new CallSite(
                      project + enclosing.replace('.', '/') + ".java",
                      frame.getMethodName(),
                      line<0?null:PowerlessArray.array(IntArray.array(line))));
                }
                return new Trace(sites.snapshot());
            }
        }
        return new TracerX();
    }
}
