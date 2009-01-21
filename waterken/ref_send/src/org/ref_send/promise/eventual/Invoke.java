// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.joe_e.inert;
import org.joe_e.array.ConstArray;
import org.joe_e.reflect.Reflection;

/**
 * Implementation plumbing that users should ignore.
 */
public final class
Invoke<T> extends Do<T,Object> implements Serializable {
    static private final long serialVersionUID = 1L;
    
    protected final Method method;
    private   final ConstArray<?> argv;
    
    /**
     * Constructs a pending invocation.
     * @param method    method to invoke
     * @param argv      invocation arguments
     */
    public
    Invoke(final Method method, final @inert ConstArray<?> argv) {
        this.method = method;
        this.argv = argv;
    }
    
    public Object
    fulfill(final T object) throws Exception {
        // AUDIT: call to untrusted application code
        return Reflection.invoke(method, object,
            null == argv ? null : argv.toArray(new Object[argv.length()]));
    }
}
