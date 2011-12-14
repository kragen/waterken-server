// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.joe_e.Powerless;
import org.joe_e.reflect.Reflection;

/**
 * Indicates the class provided to {@link Eventual#spawn spawn} is not a Maker.
 * <p>
 * A Maker class <strong>MUST</strong>:
 * </p>
 * <ul>
 * <li>be declared in a {@linkplain org.joe_e.IsJoeE Joe-E} package</li>
 * <li>be <code>public</code></li>
 * <li>have a single <code>public static</code> method named
 *     "<code>make</code>"</li>
 * </ul>
 */
public class
NotAMaker extends NullPointerException implements Powerless {
    static private final long serialVersionUID = 1L;
    
    public
    NotAMaker() {}

    private
    NotAMaker(final Class<?> maker) {
        super(Reflection.getName(maker));
    }
    
    /**
     * Finds a Maker's make method.
     * @param maker maker type
     * @return corresponding make method
     * @throws NotAMaker    no make method found
     */
    static public Method
    dispatch(final Class<?> maker) throws NotAMaker {
        Method make = null;
        for (final Method m : Reflection.methods(maker)) {
            if ("make".equals(m.getName()) && maker == m.getDeclaringClass() &&
                    Modifier.isStatic(m.getModifiers())) {
                if (null != make) { throw new NotAMaker(maker); }
                make = m;
            }
        }
        if (null == make) { throw new NotAMaker(maker); }
        return make;
    }
}
