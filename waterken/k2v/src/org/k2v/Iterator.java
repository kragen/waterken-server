// Copyright 2012 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A {@link Folder} iterator.
 */
public interface Iterator extends java.util.Iterator<ByteBuffer> {

  /**
   * Moves to the next {@link Value}.
   * @return key of the next {@link Value}
   */
  ByteBuffer readNext() throws IOException;
  
  /**
   * Reads the {@link Value} for the last {@linkplain #readNext read key}.
   */
  Value readValue() throws IOException;
}
