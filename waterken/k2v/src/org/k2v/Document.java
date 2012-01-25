// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v;

import java.io.InputStream;

/**
 * A byte stream.
 */
public abstract class Document extends InputStream implements Value {
  
  /**
   * stream length in bytes
   */
  public final long length;
  
  /**
   * @param length  {@link #length}
   */
  public Document(final long length) {
    this.length = length;
  }
}
