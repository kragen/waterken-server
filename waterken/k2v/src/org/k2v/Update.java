// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A {@link K2V} {@linkplain K2V#update() update transaction}.
 */
public interface Update extends Closeable {
  
  /**
   * Terminates the update and ensures any held locks have been released.
   * <p>Call this method with code like:
   * <pre>{@code .
   *  final Update update = db.update();
   *  try {
   *    // Do update operations here.
   *    ...
   *    update.commit();
   *  } finally {
   *    update.close();
   *  }
   * }</pre>
   * <p>If there was not a successful {@link #commit()}, any changes are
   * discarded.
   */
  void close();
  
  /**
   * Commits all changes or does nothing if there were no changes.
   */
  void commit() throws IOException;

  /**
   * Updates a {@link Value}.
   * <p>If the {@link Folder} does not exist, it and its parents will be
   * created.
   * @param folder  {@link Folder} to update
   * @param key     key for {@link Document} to create or update
   * @return corresponding {@link Document} output stream
   */
  OutputStream open(Folder folder, byte[] key) throws IOException;
  
  /**
   * Gets an updated {@link Folder} reference.
   * <p>Use the returned {@link Folder} in a subsequent {@link K2V#query()} to
   * ensure access to the updated state.
   * <p>If the {@link Folder} does not exist, it and its parents will be
   * created.
   * @param folder  {@link Folder} to touch
   * @return updated {@link Folder}
   */
  Folder touch(Folder folder) throws IOException;
  
  /**
   * Creates a child {@link Folder}.
   * <p>If the {@link Folder} does not exist, it and its parents will be
   * created.
   * @param folder  {@link Folder} to update
   * @param key     key for child {@link Folder} to create or open
   * @return corresponding sub-{@link Folder}
   */
  Folder nest(Folder folder, byte[] key) throws IOException;
}
