// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v;

/**
 * A set of {@link Value} keyed by byte string.
 * <p>A {@link Folder} can be reused across {@linkplain K2V#query() queries} and
 * {@linkplain K2V#update() updates} to avoid repeatedly
 * {@linkplain Query#find finding} it.
 */
public interface Folder extends Value {}
