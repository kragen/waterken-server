// Copyright 2008 Regents of the University of California.  May be used
// under the terms of the revised BSD license.  See LICENSING for details.
package org.joe_e.var;

import java.io.Serializable;

import org.joe_e.Equatable;

/**
 * A variable that can be set only once.
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
    plan() { return new Milestone<T>(); }
    
    // org.joe_e.var.Milestone interface

    /**
     * Has the milestone been {@linkplain #mark marked}?
     * @return <code>true</code> if {@linkplain #mark marked},
     *         else <code>false</code>
     */
    public boolean
    is() { return null != marker; }
    
    /**
     * Gets the milestone marker.
     * @return marker, or <code>null</code> if not {@linkplain #mark marked}
     */
    public T
    get() { return marker; }

    /**
     * Marks the milestone.
     * @param marker    initialized value of the variable
     * @return <code>false</code> if previously marked, else <code>true</code>
     */
    public boolean
    mark(final T marker) {
        if (null == marker) { throw new NullPointerException(); }
        if (null != this.marker) { return false; }
        this.marker = marker;
        return true;
    }
}
