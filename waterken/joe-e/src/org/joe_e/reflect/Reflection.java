// Copyright 2006-2008 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.regex.Pattern;

import org.joe_e.IsJoeE;
import org.joe_e.array.PowerlessArray;
import org.joe_e.taming.Policy;

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

    static final Pattern UNQUALIFY = 
        Pattern.compile("[^\\(<> ]*\\.([^<> \\.]*)");
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
    static public Field field(final Class<?> type, final String name) 
                                        throws NoSuchFieldException {
        return type.getField(name);
    }

    /**
     * Gets all public fields.
     * <p>
     * This method wraps {@link Class#getFields}.
     * </p>
     * @param type  object type
     * @return described fields
     */
    static public PowerlessArray<Field> fields(final Class<?> type) {
        final Field[] fs = type.getFields();

        // Sort the members to preserve determinism.
        Arrays.sort(fs, new Comparator<Field>() {
            public int compare(final Field a, final Field b) {
                int diff = a.getName().compareTo(b.getName());
                if (diff == 0) {
                    diff = a.getDeclaringClass().getName().compareTo(
                               b.getDeclaringClass().getName());
                    // Class.getName() fine as long as fields belonging to
                    // proxy classes are never safe().
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
    static public <T> Constructor<T> constructor(final Class<T> type, final Class<?>... args)
                                        throws NoSuchMethodException {
        return type.getConstructor(args);
    }

    
    /**
     * Gets all declared public constructors.
     * <p>
     * This method wraps {@link Class#getConstructors}.
     * </p>
     * @param type class to search
     * @return all public constructors
     */
//    static public <T> PowerlessArray<Constructor<T>> 
//                                        constructors(final Class<T> type) {
//  Although more expressive, this form is not used as it would preclude doing
//  anything with the result without casting it to just PowerlessArray<?> or
//  suppressing an unchecked cast warning at the point of use.

    static public PowerlessArray<Constructor<?>> 
                                        constructors(final Class<?> type) {
    
        final Constructor<?>[] cs = type.getConstructors();

        // Sort the members to preserve determinism.
        Arrays.sort(cs, new Comparator<Constructor<?>>() {
            public int compare(final Constructor<?> a, final Constructor<?> b) {
                final Class<?>[] pa = a.getParameterTypes();
                final Class<?>[] pb = b.getParameterTypes();
                int diff = pa.length - pb.length;
                for (int i = 0; diff == 0 && i < pa.length; ++i) {
                    diff = pa[i].getName().compareTo(pb[i].getName());
                    // OK since compiled types don't change
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
    static public Method method(final Class<?> type, final String name, 
                                final Class<?>... args) 
                                        throws NoSuchMethodException {
        return type.getMethod(name, args);
    }

    /**
     * Gets all public methods.
     * <p>
     * This method wraps {@link Class#getMethods}.
     * </p>
     * @param type object type
     * @return described methods
     */
    static public PowerlessArray<Method> methods(final Class<?> type) {
        final Method[] ms = type.getMethods();

        // Sort the members to preserve determinism.
        Arrays.sort(ms, new Comparator<Method>() {
            public int compare(final Method a, final Method b) {
                int diff = a.getName().compareTo(b.getName());
                if (diff == 0) {
                    diff = a.getDeclaringClass().getName().compareTo(
                            b.getDeclaringClass().getName());
                    // Class.getName() fine as long as methods belonging to
                    // proxy classes are never safe().
                    if (diff == 0) {
                        final Class<?>[] pa = a.getParameterTypes();
                        final Class<?>[] pb = b.getParameterTypes();
                        if (pa.length != pb.length) {
                            diff = pa.length - pb.length;
                        }
                        for (int i = 0; diff == 0 && i < pa.length; ++i) {
                            diff = pa[i].getName().compareTo(pb[i].getName());
                        }
                        // OK since compiled types don't change
                    }
                }
                return diff;
            }
        });

        return PowerlessArray.array(ms);
    }

    /**
     * Get the name of the entity represented by a <code>Class</code> object,
     * in the same format as returned by {@link Class#getName()}.  This wrapper
     * exists to avoid exposing the number of proxy interfaces that have been
     * generated.
     * @param c the class to get the name of
     * @return the name of class <code>c</code>
     * @throws IllegalArgumentException if <code>c</code> is a proxy class
     */
    static public String getName(Class<?> c) {
        if (java.lang.reflect.Proxy.isProxyClass(c)) {
            throw new IllegalArgumentException("Can't get the name of a " +
                                               "proxy class.");
        } else {
            return c.getName();
        }
    }
    
    /**
     * Clears the stack trace on an exception.
     * @param e exception to modify
     */
    static public void clearStackTrace(final Throwable e) {
        e.setStackTrace(new StackTraceElement[] {});
    }
    
    /**
     * boot class loader
     */
    // static private final ClassLoader boot = Runnable.class.getClassLoader();
   
    /**
     * Is the given member allowed to be accessed by Joe-E code?
     * @param member    candidate member
     * @return <code>true</code> if the member may be used by Joe-E code,
     *         else <code>false</code>
     */
    static private boolean safe(final Member member) {
        final Class<?> declarer = member.getDeclaringClass();
        // safe if declared in a Joe-E package
        final Package pkg = declarer.getPackage();
        // getPackage returns null for proxy classes
        if (pkg != null && pkg.isAnnotationPresent(IsJoeE.class)) {
            return true;
        }
        
        // getName() is the binary name, possibly with $'s
        StringBuilder sb = new StringBuilder(declarer.getName());
        if (member instanceof Field) {
            sb.append("." + member.getName());
            return Policy.fieldEnabled(sb.toString());
        }
        else if (member instanceof Constructor<?>) {
            String stringForm = member.toString();
            String args = stringForm.substring(stringForm.indexOf('('),
                                               stringForm.indexOf(')') + 1);
            sb.append(UNQUALIFY.matcher(args).replaceAll("$1"));
            return Policy.constructorEnabled(sb.toString());
        } else { // member instanceof Method
            sb.append("." + member.getName());
            String stringForm = member.toString();
            String args = stringForm.substring(stringForm.indexOf('('),
                                               stringForm.indexOf(')') + 1);
            sb.append(UNQUALIFY.matcher(args).replaceAll("$1"));
            return Policy.methodEnabled(sb.toString());
        }
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
            throw new IllegalAccessException(field.toString());
        }
        if (!safe(field)) {throw new IllegalAccessException(field.toString());}
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
            throw new IllegalAccessException(field.toString());
        }
        if (!safe(field)) {throw new IllegalAccessException(field.toString());}
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
    static public <T> T construct(final Constructor<T> ctor, final Object... args)
                                        throws Exception {
        if (!Modifier.isPublic(ctor.getDeclaringClass().getModifiers())) {
            throw new IllegalAccessException(ctor.toString());
        }
        if (!safe(ctor)) { throw new IllegalAccessException(ctor.toString()); }
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
            throw new IllegalAccessException(method.toString());
        }
        if (!safe(method)) {
            throw new IllegalAccessException(method.toString());
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
