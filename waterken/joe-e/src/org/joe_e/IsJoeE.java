// Copyright 2008 Regents of the University of California.  May be used 
// under the terms of the revised BSD license.  See LICENSING for details.
/** 
 * @author Adrian Mettler 
 */
package org.joe_e;
import java.lang.annotation.*;

/**
 * Package annotation to use for packages containing Joe-E code. All classes in
 * the package will be checked if this annotation is applied. (Package 
 * annotations can be specified in a file named <code>package-info.java</code>
 * with a line like "<code>@org.joe_e.IsJoeE package package.name;</code>".)
 */
@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
public @interface IsJoeE {

}
