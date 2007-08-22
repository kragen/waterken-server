// Copyright 2007 Regents of the University of California.  May be used
// under the terms of the revised BSD license.  See LICENSING for details.
/**
 * @author Adrian Mettler
 */
package org.joe_e;

/**
 * Marker interface for annotating classes whose instances are permitted
 * to be compared using the <code>==</code> and <code>!=</code> operators.
 * These operators compare using the address of the object and thus expose
 * object identity.  Objects that do not implement Equatable are prohibited
 * (by Joe-E) from being compared using these comparison operators.
 * <p>
 * A class that implements Equatable must not implement Selfless.
 * <p>
 * This interface has no members.
 * 
 * @see Selfless
 */
public interface Equatable {

}
