// Copyright 2005-08 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e;
import org.joe_e.taming.Policy;

/**
 * Joe-E core library functions.  These provide fundamental features, similar
 * to the methods in <code>java.lang.System</code>, such as the interface to the
 * overlay type system.
 */
public class JoeE {
    
    private JoeE() {}

    /**
     * Tests whether the specified object belongs to the specified type in the
     * overlay type system. The equivalent of the Java <code>instanceof</code>
     * operator, for the overlay type system.  
     * 
     * @param obj  the object to test
     * @param type the type to test membership of
     * @return true if the specified object belongs to the specified type
     *  in the overlay type system.
     */
    static public boolean instanceOf(Object obj, Class<?> type) {
        return obj != null && isSubtypeOf(obj.getClass(), type);
    }

    /**
     * Tests whether the first class is a subtype of the second in the overlay
     * type system.  
     * 
     * @param c1 the potential subtype
     * @param c2 the potential supertype
     * @return true if the first argument is a subtype of the second in the
     *  overlay type system
     */
    /*
     * It might be hard to believe at first that an algorithm this simple
     * can take into account all transitive dependencies correctly,
     * but here is the key fact that makes it work:
     * if C honorarily implements marker interface I, and D is a
     * subclass of C, then either (1) D is from the Java library,
     * in which case the honorary implementation guarantees that D
     * will also be marked as honorarily implementing I; or (2) D is
     * user code, in which case the Joe-E verifier requires D to explicitly
     * implement I (in the Java type system).  In either case, this
     * accounts for all transitive dependencies: in case (1), the call
     * to honorarilyImplements() take care of transitive subtyping;
     * in case (2), isAssignableFrom() takes care of it.
     */
    static public boolean isSubtypeOf(Class<?> c1, Class<?> c2) {
        if (c2.isAssignableFrom(c1)) {
            return true;
        } else {
            return Policy.hasHonorary(c1.getName(), c2.getName());
        }
    }
    
    /**
     * This field holds the ErrorHandler to be invoked when 
     * <code>abort()</code> is called.
     * Trusted infrastructure (non­­-Joe-E) code may change the abort behavior 
     * by using unsafe reflection to modify the value of this field.
     */
    static private ErrorHandler handler = new SystemExit();
    
    /**
     * Aborts the current flow of control.  This method invokes the currently
     * registered error handler, which should preclude continued execution of 
     * Joe-E code.
     * @param reason    reason for the abort
     * @return an error to throw
     */
    static public Error abort(final Error reason) {
        while (true) {
            try {
                return handler.handle(reason);
            } catch (final Throwable e) {
                // Keep trying...
            }
        }
    }

    /**
     * Is the object one-level deep immutable?
     * @param x candidate object
     * @return <code>true</code> if <code>x</code> is one-level deep immutable,
     *         <code>false</code> if it might not be
     */
    static public boolean
    isFrozen(final Object x) {
        return null == x ||
               instanceOf(x, Selfless.class) ||
               instanceOf(x, Immutable.class);
    }
}
