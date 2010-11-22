// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the deserialization constructor.
 * <p>
 * This annotation MUST only be put on a single {@code public} constructor of a 
 * {@code public} class in a {@linkplain org.joe_e.IsJoeE Joe-E package}. Each
 * parameter of the constructor MUST be {@linkplain name annotated} with the
 * name of the corresponding {@code public final} instance field.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface
deserializer {}
