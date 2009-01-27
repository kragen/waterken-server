// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.trace.application;

import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Proxy;

import org.joe_e.Struct;
import org.joe_e.array.IntArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.log.CallSite;
import org.ref_send.log.Trace;
import org.waterken.trace.Tracer;

/**
 * Produces a stack trace composed of only calls initiated by application code.
 */
public final class
ApplicationTracer {

    /**
     * Constructs an instance.
     * @param code  application class loader
     */
    static public Tracer
    make(final ClassLoader code) {
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
                    
                    // Is this frame from application code or system code?
                    boolean included = false;
                    try {
                        final Class<?> c = code.loadClass(frame.getClassName());
                        if (!Proxy.isProxyClass(c)) {
                            final ClassLoader application = c.getClassLoader();
                            final ClassLoader system =
                                ApplicationTracer.class.getClassLoader();
                            for(ClassLoader i=code; i!=system; i=i.getParent()){
                                if (application == i) {
                                    included = true;
                                    break;
                                }
                            }
                        }
                    } catch (final ClassNotFoundException e) {}
                    if (!included) {
                        if (0 == sites.length()) { continue; }
                        break;
                    }
                    
                    // Describe the application stack frame.
                    final String enclosing; {
                        final String name = frame.getClassName();
                        final int end = name.indexOf('$');
                        enclosing = -1 == end ? name : name.substring(0, end);
                    }
                    final String typename; {
                        final String name = frame.getClassName();
                        typename = name.substring(name.lastIndexOf('.') + 1);
                    }
                    final int line = frame.getLineNumber();
                    sites.append(new CallSite(
                      enclosing.replace('.', '/') + ".java",
                      typename + "." + frame.getMethodName(),
                      line>1?PowerlessArray.array(IntArray.array(line)):null));
                }
                return new Trace(sites.snapshot());
            }
        }
        return new TracerX();
    }
}
