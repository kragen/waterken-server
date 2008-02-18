// Copyright 2006-2007 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Comparator;

import org.joe_e.array.PowerlessArray;

/**
 * The reflection interface.
 * <p>
 * This API provides reflective access to all the public constructors, public
 * fields and public methods of public Joe-E classes and interfaces. The API
 * provides no more permission than is provided by static Joe-E program code.
 * If you can do something with the reflection API, you could also have done
 * it using static Joe-E code. The only difference is expressivity.
 * </p>
 */
public final class Reflection {
    private Reflection() {}

    /*
     * Methods for obtaining reflective objects
     */
        
    /**
     * Gets a public field.
     * <p>
     * This method wraps {@link Class#getField}.
     * </p>
     * @param type  class to search
     * @param name  field name
     * @return described field
     * @throws NoSuchFieldException no matching field found
     */
    static public Field field(final Class type, final String name) 
                                        throws NoSuchFieldException {
        final Field f = type.getField(name);
        if (!safe(f)) { 
            throw new NoSuchFieldException(); 
        }
        return f;
    }

    /**
     * Gets all public fields.
     * <p>
     * This method wraps {@link Class#getFields}.
     * </p>
     * @param type  object type
     * @return described fields
     */
    static public PowerlessArray<Field> fields(final Class type) {
        Field[] fs = type.getFields();

        // Filter the members.
        int n = 0;
        for (final Field f : fs) {
            if (safe(f)) {
                fs[n++] = f; 
            }
        }

        // Sort the members to preserve determinism.
        if (fs.length != n) { 
            System.arraycopy(fs, 0, fs = new Field[n], 0, n);
        }
        Arrays.sort(fs, new Comparator<Field>() {
            public int compare(final Field a, final Field b) {
                int diff = a.getName().compareTo(b.getName());
                if (diff == 0) {
                    diff = a.getDeclaringClass().getName().compareTo(
                               b.getDeclaringClass().getName());
                }
                return diff;
            }
        });

        return PowerlessArray.array(fs);
    }

    /**
     * Gets a public constructor.
     * <p>
     * This method wraps {@link Class#getConstructor}.
     * </p>
     * @param type  class to search
     * @param args each parameter type
     * @return described constructor
     * @throws NoSuchMethodException    no matching constructor found
     */
    static public Constructor constructor(final Class type, final Class... args)
                                        throws NoSuchMethodException {
        final Constructor c = type.getConstructor(args);
        if (!safe(c)) {
            throw new NoSuchMethodException();
        }
        return c;
    }

    /**
     * Gets all declared public constructors.
     * <p>
     * This method wraps {@link Class#getConstructors}.
     * </p>
     * @param type class to search
     * @return all public constructors
     */
    static public PowerlessArray<Constructor> constructors(final Class type) {
        Constructor[] cs = type.getConstructors();

        // Filter the members.
        int n = 0;
        for (final Constructor c : cs) {
            if (safe(c)) { 
                cs[n++] = c;
            }
        }

        // Sort the members to preserve determinism.
        if (cs.length != n) { 
            System.arraycopy(cs, 0, cs = new Constructor[n], 0, n); 
        }
        Arrays.sort(cs, new Comparator<Constructor>() {
            public int compare(final Constructor a, final Constructor b) {
                final Class[] pa = a.getParameterTypes();
                final Class[] pb = b.getParameterTypes();
                int diff = pa.length - pb.length;
                for (int i = 0; diff == 0 && i < pa.length; ++i) {
                    diff = pa[i].getName().compareTo(pb[i].getName());
                }
                return diff;
            }
        });

        return PowerlessArray.array(cs);
    }

    /**
     * Gets a public method.
     * <p>
     * This method wraps {@link Class#getMethod}.
     * </p>
     * @param type  class to search
     * @param name  method name
     * @param args each parameter type
     * @return described method
     * @throws NoSuchMethodException    no matching method found
     */
    static public Method method(final Class type, final String name, 
                                final Class... args) 
                                        throws NoSuchMethodException {
        final Method r = type.getMethod(name, args);
        if (!safe(r)) { 
            throw new NoSuchMethodException(); 
        }
        return r;
    }

    /**
     * Gets all public methods.
     * <p>
     * This method wraps {@link Class#getMethods}.
     * </p>
     * @param type object type
     * @return described methods
     */
    static public PowerlessArray<Method> methods(final Class type) {
        Method[] ms = type.getMethods();

        // Filter the members.
        int n = 0;
        for (final Method m : ms) { 
            if (safe(m)) { 
                ms[n++] = m; 
            }
        }

        // Sort the members to preserve determinism.
        if (ms.length != n) { 
            System.arraycopy(ms, 0, ms = new Method[n], 0, n); 
        }
        Arrays.sort(ms, new Comparator<Method>() {
            public int
            compare(final Method a, final Method b) {
                int diff = a.getName().compareTo(b.getName());
                if (diff == 0) {
                    diff = a.getDeclaringClass().getName().compareTo(
                            b.getDeclaringClass().getName());
                    if (diff == 0) {
                        final Class[] pa = a.getParameterTypes();
                        final Class[] pb = b.getParameterTypes();
                        if (pa.length != pb.length) {
                            diff = pa.length - pb.length;
                        }
                        for (int i = 0; diff == 0 && i < pa.length; ++i) {
                            diff = pa[i].getName().compareTo(pb[i].getName());
                        }
                    }
                }
                return diff;
            }
        });

        return PowerlessArray.array(ms);
    }
 
    /**
     * Get the name of a class.  Wrapper to avoid exposing the number of
     * proxy interfaces generated.
     */
    static public String getName(Class c) {
        if (java.lang.reflect.Proxy.isProxyClass(c)) {
            throw new IllegalArgumentException("Can't get the name of a " +
                                               "proxy class.");
        } else {
            return c.getName();
        }
    }

    /**
     * boot class loader
     */
    static private final ClassLoader boot = Runnable.class.getClassLoader();

    /**
     * Is the given member allowed to be accessed by Joe-E code?
     * @param member    candidate member
     * @return <code>true</code> if the member may be used by Joe-E code,
     *         else <code>false</code>
     */
    /* 
     * TODO: Current implementation is a temporary hack.  It is wildly
     * over-conservative with library functions (can't call anything except a
     * few special cases).  It is also potentially unsafe, in cases where
     * any non-boot-classloader code is disallowed by taming.  The correct way
     * to implement this requires runtime access to the taming database.
     */
    static private boolean safe(final Member member) {
        final Class declarer = member.getDeclaringClass();
        return declarer == Runnable.class 
            || (declarer == Object.class
                && (member.getName().equals("getClass")
                    || member.getName().equals("equals")
                    || member.getName().equals("Object")))
            || (declarer == RuntimeException.class
                && member instanceof Constructor)
            || (declarer == NullPointerException.class
                && member instanceof Constructor)
            || (declarer == ClassCastException.class
                && member instanceof Constructor)
            || (declarer.getClassLoader() != boot
                // prevent cheating the checks on invocation handlers
                && !(member instanceof Constructor
                     && Proxy.class.isAssignableFrom(declarer)));
    }

    /*
     * Methods for using reflective objects
     */
    
    /**
     * Gets the value of a field.
     * @param field  field to access
     * @param self  target object
     * @return field value
     * @throws IllegalAccessException   <code>field</code> is inaccessible
     */
    static public Object get(final Field field, final Object self) 
                                        throws IllegalAccessException {
        if (!Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
            throw new IllegalAccessException();
        }
        return field.get(self);
    }

    /**
     * Sets the value of a field.
     * @param field  field to access
     * @param self  target object
     * @param value new value
     * @throws IllegalAccessException   <code>field</code> is inaccessible
     */
    static public void set(final Field field, final Object self, 
                           final Object value) throws IllegalAccessException {
        if (!Modifier.isPublic(field.getDeclaringClass().getModifiers())) {
            throw new IllegalAccessException();
        }
        field.set(self, value);
    }

    /**
     * Invokes a reflected constructor.
     * @param ctor  constructor to invoke
     * @param args   each argument
     * @return constructed object
     * @throws IllegalAccessException  <code>ctor</code> is inaccessible
     * @throws ClassCastException  <code>ctor.newInstance()</code> throws an 
     *    <code>IllegalArgumentException</code>, usually due to mismatched types   
     * @throws Exception    an exception thrown by the invoked constructor
     */
    static public Object construct(final Constructor ctor, final Object... args)
                                        throws Exception {
        if (!Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) {
            throw new IllegalAccessException();
        }
        try {
            return ctor.newInstance(args);
        } catch (final IllegalArgumentException e) {
            throw new ClassCastException(e.getMessage());
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) { 
                throw (Error) cause;
            }
            throw (Exception) cause;
        }
    }

    /**
     * Invokes a reflected method.
     * @param method  method to invoke
     * @param self  target object
     * @param args   each argument
     * @return invocation return
     * @throws IllegalAccessException  <code>method</code> is inaccessible
     * @throws ClassCastException  <code>method.invoke()</code> throws an 
     *    <code>IllegalArgumentException</code>, usually due to mismatched types   
     * @throws Exception    an exception thrown by the invoked method
     */
    static public Object invoke(final Method method, final Object self,
                                final Object... args) throws Exception {
        if (!Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            throw new IllegalAccessException();
        }
        try {
            return method.invoke(self, args);
        } catch (final IllegalArgumentException e) {
            throw new ClassCastException();
        } catch (final InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof Error) { 
                throw (Error) cause;
            }
            throw (Exception) cause;
        }
    }
}
