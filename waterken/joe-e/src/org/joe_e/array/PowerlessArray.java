// Copyright 2006 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e.array;

import org.joe_e.JoeE;
import org.joe_e.Powerless;

/**
 * An immutable array containing powerless objects.
 * 
 * @param <E> the element type of objects contained in the array
 */
public class PowerlessArray<E> extends ImmutableArray<E> implements Powerless {
    static private final long serialVersionUID = 1L;

    /**
     * Package-scope back-door constructor for use by subclasses that
     * override all methods that make use of the field arr.  Nullity of arr is
     * used to distinguish between instances with which this class must interact
     * by using the public interface rather than through their arr field.
     */
	PowerlessArray(final Object[] arr) {
		super(arr); 
	}
    
    /**
     * Constuct a {@link PowerlessArray}.  The type will be 
     * @param values    each value, or an array of values
     * @throws ClassCastException if the runtime component type of 
     *     <code>values</code> is not powerless in the overlay type system
     */
    static public <E> PowerlessArray<E> array(final E... values) {
        final Class e = values.getClass().getComponentType();
        if (!JoeE.isSubtypeOf(e, Powerless.class)) {
            throw new ClassCastException(e.getName() + " is not Powerless");
        }
        return new PowerlessArray<E>(values.clone());
    }
    
    /**
     * Return a new <code>PowerlessArray</code> that contains the same elements
     * as this one but with a new element added to the end.
     * @param newE an element to add
     * @return the new array
     * @throws ClassCastException if <code>newE</code> is not powerless 
     */
    public PowerlessArray<E> with(E newE) {
        if (!JoeE.instanceOf(newE, Powerless.class)) {
            throw new ClassCastException(newE.getClass().getName() + "is not Powerless");
        }
        // We use a new Object array here, because we don't know the static type
        // of E that was used; it may not match the dynamic component type of
        // arr due to array covariance.
        final Object[] newArr = new Object[arr.length + 1];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        newArr[arr.length] = newE;
        return new PowerlessArray<E>(newArr);
    }
}
