// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v;

import java.io.Closeable;
import java.io.IOException;

/**
 * Transactional interface to a {@link Folder}.
 * <p>Instances of this type are thread-safe.
 */
public interface K2V extends Closeable {
  
  /**
   * Close after every currently running {@link #query()} and {@link #update()}
   * has executed to its {@link Closeable#close()}.
   */
  void close() throws IOException;

  /**
   * Creates a {@link Query} transaction that includes the results of the last
   * {@link #update()} to {@link Update#commit()}.
   * <p>{@link Query} results will not be affected by any {@link #update()}
   * whose {@link Update#commit()} occurs after the returned transaction object
   * is created.
   * <p>The returned transaction object is not thread-safe.
   */
  Query query() throws IOException;
  
  /**
   * Creates an {@link Update} transaction.
   * <p>The returned transaction object is not thread-safe.
   */
  Update update() throws IOException;
}
