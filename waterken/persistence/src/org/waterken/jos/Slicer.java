// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.joe_e.JoeE;
import org.joe_e.Selfless;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Deferred;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.db.Root;

/**
 * Slices an object graph at selfish object references.
 */
final class
Slicer extends ObjectOutputStream {
    
    private final Object value;
    private final Root root;
    
    Slicer(final Object value, final Root root,
           final OutputStream out) throws IOException {
        super(out);
        this.value = value;
        this.root = root;
        enableReplaceObject(true);
    }
    
    static private final Class<?> Detachable = Eventual.detach(0).getClass();

    protected Object
    replaceObject(Object x) throws IOException {
        final Class<?> type = null != x ? x.getClass() : Void.class;
        // BEGIN: persistence for non-Serializable types
        if (Field.class == type) {
            x = new FieldWrapper((Field)x);
        } else if (Method.class == type) {
            x = new MethodWrapper((Method)x);
        } else if (Constructor.class == type){
            x = new ConstructorWrapper((Constructor<?>)x);
        } else if (BigInteger.class == type) {
            x = new BigIntegerWrapper((BigInteger)x);
        } else if (BigDecimal.class == type) {
            x = new BigDecimalWrapper((BigDecimal)x);
        // END: persistence for non-Serializable types
        } else if (value == x) {
        // BEGIN: slicing of the object graph into trees
        } else if (Detachable == type) {
            x = new Faulting(root,
                    root.export(Eventual.near((Fulfilled<?>)x), false));
        } else if (Deferred.WeakPromise == type) {
            final Object referent;
            try {
                referent = ((Volatile<?>)x).cast();
            } catch (final Exception e) { throw new AssertionError(e); }
            x = new Faulting(root, root.export(referent, true));
        } else if (!inline(type)) {
            if (value instanceof Throwable &&
                StackTraceElement.class == type.getComponentType()) {
                // This must be the contained stack trace array. Just let it
                // go by, since it acts like it's selfless.
            } else {
                x = new Splice(root.export(x, false));
            }
        }
        // END: slicing of the object graph into trees
        return x;
    }

    /**
     * Can the object's creation identity be ignored?
     * @param x candidate object
     * @return true if the object's creation identity need not be preserved,
     *         false if it MUST be preserved
     */
    static protected boolean
    inline(final Class<?> type) {
        return type == Void.class || type == Class.class || 
               type == StackTraceElement.class ||
               JoeE.isSubtypeOf(type, Selfless.class);
    }
}
