// Copyright 2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.reflect;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import org.joe_e.Equatable;
import org.joe_e.Immutable;
import org.joe_e.JoeE;
import org.joe_e.Powerless;
import org.joe_e.Selfless;

/**
 * The dynamic proxy interface.  This is a wrapper around Java's dynamic proxy
 * features provided by <code>java.lang.reflect.Proxy</code>.
 */
public final class Proxies {

    private Proxies() {}

    /**
     * Extract the <code>InvocationHandler</code> from a Proxy
     * @param proxy {@link #proxy proxy} instance
     * @return the invocation handler for the proxy
     */
    static public InvocationHandler getHandler(final Proxy proxy) { 
        return Proxy.getInvocationHandler(proxy); 
    }

    /**
     * The boot class loader.
     */
    static private final ClassLoader boot = Runnable.class.getClassLoader();

    /**
     * Constructs a dynamic proxy.
     * @param handler invocation handler
     * @param interfaces each implemented interface
     * @return dynamic proxy
     * @throws ClassCastException   a restriction on the
     *                              <code>types</code> is violated
     * @throws NullPointerException an argument is <code>null</code>
     */
    static public Object proxy(final InvocationHandler handler,
                               final Class<?>... interfaces) 
                                        throws ClassCastException { 
        if (handler == null || interfaces == null) {
            throw new NullPointerException();
        }
        
        // Determine the classloader.
        ClassLoader proxyLoader = boot;
        
        boolean equatable = false;  // Was the Equatable type claimed?
        boolean selfless = false;   // Was the Selfless type claimed?
        boolean immutable = false;  // Was the Immutable type claimed?
        boolean powerless = false;  // Was the Powerless type claimed?
        
        for (final Class<?> i : interfaces) {
            
            // Can only implement public interfaces.
            if (!Modifier.isPublic(i.getModifiers())) {
                throw new ClassCastException();
            }
            
            // Perform Joe-E auditor checks.
            if (!powerless && JoeE.isSubtypeOf(i, Powerless.class)) {
                if (JoeE.instanceOf(handler, Powerless.class)) {
                    powerless = true;
                    immutable = true;
                } else {
                    throw new ClassCastException();
                }
            }
            if (!immutable && JoeE.isSubtypeOf(i, Immutable.class)) {
                if (JoeE.instanceOf(handler, Immutable.class)) {
                    immutable = true;
                } else {
                    throw new ClassCastException();
                }
            }
            if (!equatable && JoeE.isSubtypeOf(i, Equatable.class)) {
                // No additional checks are needed here, as we know that Proxy
                // is not Selfless.  An Equatable Proxy can have an identity-
                // based equals() method if it so chooses by casting the Proxy
                // object to Equatable.
                if (selfless) { 
                    throw new ClassCastException();
                }
                equatable = true;
            }
            if (!selfless && JoeE.isSubtypeOf(i, Selfless.class)) {
                // No additional checks are needed here, because Object's
                // equals() method is not available from the invocation handler
                // and Proxy does not expose this method.  Dynamic proxies are
                // thus the only Selfless user class whose direct supertype is
                // neither a Selfless class nor Object.
                if (equatable) { 
                    throw new ClassCastException();
                }
                selfless = true;
            }
            
            final ClassLoader interfaceLoader = i.getClassLoader();

            // TODO: This will change if there is runtime info on banned
            // interfaces or if a Library interface is added.
            // if (interfaceLoader == boot && i != Runnable.class) { 
            //    throw new ClassCastException();
            //}
    
            // Prefer any classloader over the bootstrap classloader.
            if (proxyLoader == boot) {
                proxyLoader = interfaceLoader;
            }
        }
        try {
            return Proxy.newProxyInstance(proxyLoader, interfaces, handler);
        } catch (final IllegalArgumentException e) {
            throw new ClassCastException(e.getMessage());
        }
    }

    /**
     * Returns <code>true</code> if the argument is an interface that can be
     * implemented by a Proxy using <code>proxy()</code>.
     * @param type  candidate interface
     * @return <code>true</code> if a Joe-E type, else <code>false</code>
     */
    static public boolean isImplementable(final Class<?> type) {
        return type.isInterface() && Modifier.isPublic(type.getModifiers());
        
        // return Runnable.class == type ||
        //       (type.isInterface() && type.getClassLoader() != boot &&
        //        Modifier.isPublic(type.getModifiers())); 
        // Can't consult taming database as it doesn't have proper info
        // TODO: This will change if there are interfaces the implementation of
        // which must be banned (e.g. Library)
    }
}
