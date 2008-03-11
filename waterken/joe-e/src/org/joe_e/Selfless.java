// Copyright 2006-07 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e;

/**
 * Marker interface for annotating classes that are indistinguishable from
 * a shallow copy of themselves.
 * <p>
 * Joe-E requires that classes that implement this interface must
 * meet all of the following obligations:
 * <ol>
 * <li> All instance fields must be final.
 * <li> The class cannot be equatable.
 * <li> The object identity of instances of the class is not visible  This can
 *   be satisfied by either of:
 *   <ol>
 *   <li> The superclass implements Selfless; or,
 *   <li> The class's superclass is java.lang.Object, the class overrides
 *        <code>equals()</code>, and doesn't call <code>super.equals()</code>. 
 *   </ol>
 * <li> The object provides a hash code that does not reveal object identity.
 *   This requirement is enforced by including hashCode() in the interface.
 * </ol>
 * <p>
 * The Joe-E verifier ensures that Joe-E code cannot distinguish a
 * shallow copy of a Selfless object from the original object.
 * 
 * @see Equatable
 */
public interface Selfless {
    int hashCode();
    /*
    boolean equals(Object obj);
    */
}