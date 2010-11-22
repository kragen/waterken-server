// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;

import org.joe_e.JoeE;
import org.joe_e.Selfless;
import org.joe_e.array.PowerlessArray;
import org.joe_e.var.Milestone;
import org.ref_send.promise.Eventual;
import org.ref_send.scope.Layout;
import org.waterken.db.Root;

/**
 * Slices an object graph at selfish object references.
 */
/* package */ final class
Slicer extends ObjectOutputStream {
    
    private final boolean weakTop;
    private final Object top;
    private final Root root;
    
    private final boolean throwableTop;
    private final Milestone<Boolean> unmanaged = Milestone.make();
    private final HashSet<String> splices = new HashSet<String>(8);
    
    Slicer(final boolean weakTop, final Object top, final Root root,
           final OutputStream out) throws IOException {
        super(out);
        this.weakTop = weakTop;
        this.top = top;
        this.root = root;
        throwableTop = top instanceof Throwable;
        enableReplaceObject(true);
    }
    
    static private final Class<?> Fulfilled;
    static private final Field isWeak;
    static private final Field state;
    static {
        try {
            Fulfilled = Eventual.ref(0).getClass();
            isWeak = Fulfilled.getDeclaredField("isWeak");
            isWeak.setAccessible(true);
            state = Fulfilled.getDeclaredField("state");
            state.setAccessible(true);
        } catch (final Exception e) { throw new AssertionError(e); }
    }

    protected Object
    replaceObject(Object x) throws IOException {
        if (x instanceof File) { unmanaged.set(true); }
        
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
        } else if (top == x) {
        // BEGIN: slicing of the object graph into trees
        } else if (throwableTop &&
     		       StackTraceElement.class == type.getComponentType()) {
			// This must be the contained stack trace array. Just let it go by,
        	// since it acts like it's selfless.
        } else if (Fulfilled == type) {
            try {
                final Object p = state.get(x);
                if (!(p instanceof Faulting)) {
                    final String name = root.export(
                        Eventual.near(p), weakTop || isWeak.getBoolean(x));
                    state.set(x, new Faulting(root, name));
                }
            } catch (final Exception e) { throw new AssertionError(e); }
        } else if (Layout.class == type ||	// intern Layout instances
        		   !inline(type)) {
            final String name = root.export(x, weakTop);
            splices.add(name);
            x = new Splice(name);
        }
        // END: slicing of the object graph into trees
        return x;
    }
    
    protected boolean
    isManaged() { return !unmanaged.is(); }
    
    protected PowerlessArray<String>
    getSplices() {return PowerlessArray.array(splices.toArray(new String[0]));}

    /**
     * Can the object's creation identity be ignored?
     * @param type  candidate object's type
     * @return true if the object's creation identity need not be preserved,
     *         false if it MUST be preserved
     */
    static protected boolean
    inline(final Class<?> type) {
        return (JoeE.isSubtypeOf(type, Selfless.class) &&
                // The eventual operator is fat and is hard referenced from
                // everywhere so don't store it inline.
                !Eventual.class.isAssignableFrom(type)) ||
               type == Fulfilled || type == Void.class || type == Class.class ||
               type == StackTraceElement.class;
    }
}
