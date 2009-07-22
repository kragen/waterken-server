// Copyright 2008 Regents of the University of California.  May be used
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.var;

import java.io.Serializable;

import org.joe_e.Equatable;

/**
 * A variable that can be set only once.
 * <p>
 * An instance of this class is like a <code>final</code> variable that can
 * either be initialized, or left uninitialized. This is useful for keeping
 * track of whether or not a particular branch of code has been executed. For
 * example, say you've got some code that operates on a set of objects.
 * Sometimes, the set is mutated, sometimes not. To remember whether or not a
 * mutation was made, the set's update method could be coded as:
 * </p>
 * <pre>
 * private final Milestone&lt;Boolean&gt; dirty = Milestone.make();
 * &hellip;
 * public void
 * add(final Object x) {
 *     objectSet.add(x);
 *     dirty.set(true);
 * }
 * </pre>
 * <p>
 * When the code is done using the set, a commit() method can check to see if
 * any updates need to be written:
 * </p>
 * <pre>
 * public void
 * commit() {
 *     if (dirty.is()) {
 *         // write out the new set
 *         &hellip;
 *     }
 * }
 * </pre>
 * <p>
 * By using this class, instead of a normal variable, you can better communicate
 * to a code reviewer that a variable is only assigned once. The variable
 * declaration alone ensures this property is enforced, without the need to
 * inspect all the code that can access the variable.
 * </p>
 * <p>
 * This class is named Milestone, since it keeps track of whether or not a
 * program's execution reached a certain point or not. This program meaningful
 * execution point is a milestone.
 * </p>
 */
public class
Milestone<T> implements Equatable, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * current state
     */
    private T marker;

    protected
    Milestone() {}
    
    /**
     * Constructs an instance.
     */
    static public <T> Milestone<T>
    make() { return new Milestone<T>(); }
    
    // org.joe_e.var.Milestone interface

    /**
     * Has the milestone been {@linkplain #set passed}?
     * @return <code>true</code> if {@linkplain #set set},
     *         else <code>false</code>
     */
    public boolean
    is() { return null != marker; }
    
    /**
     * Gets the milestone marker.
     * @return marker, or <code>null</code> if not {@linkplain #set set}
     */
    public T
    get() { return marker; }

    /**
     * Sets the milestone marker, if not already set.
     * @param marker    initialized value of the variable
     * @return <code>false</code> if previously set, else <code>true</code>
     */
    public boolean
    set(final T marker) {
        if (null == marker) { throw new NullPointerException(); }
        if (null != this.marker) { return false; }
        this.marker = marker;
        return true;
    }
}
