// Copyright 2005-06 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e;

/**
 * Content-less class used solely for its unforgeable object identity.
 * Distinct categories of tokens can be created by subclassing this class.
 * <p>
 * Note: this class implements Serializable in order to avoid preventing
 * trusted (non-Joe-E) code from serializing it.  The Java Serialization API
 * is tamed away as unsafe, and thus is not available to Joe-E code.
 */
public class Token implements Immutable, Equatable, java.io.Serializable {
    static final long serialVersionUID = 1L;    
}
