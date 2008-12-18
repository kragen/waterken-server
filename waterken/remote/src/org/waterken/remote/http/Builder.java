// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.promise.eventual.Receiver;
import org.ref_send.promise.eventual.Resolver;
import org.web_send.graph.Framework;

/**
 * Performs application specific initialization.
 */
/* package */ final class
Builder<T> extends Struct implements Receiver<ConstArray<?>>, Serializable {
    static private final long serialVersionUID = 1L;

    private final Method build;
    private final Framework framework;
    private final Resolver<T> resolver;
    
    protected
    Builder(final Method build, final Framework framework,
            final Resolver<T> resolver) {
        this.build = build;
        this.framework = framework;
        this.resolver = resolver;
    }
    
    public @SuppressWarnings("unchecked") void
    run(final ConstArray<?> optional) {
        final T v;
        try {
            final Object[] argv = new Object[1 + optional.length()];
            argv[0] = framework;
            for (int i = 0; i != optional.length(); ++i) {
                argv[1 + i] = optional.get(i);
            }
            v = (T)build.invoke(null, argv);
        } catch (final Exception e) {
            resolver.reject(e);
            return;
        }
        resolver.run(v);
    }
}
