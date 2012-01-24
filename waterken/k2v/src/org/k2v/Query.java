// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v;

import java.io.Closeable;
import java.io.IOException;

/**
 * A {@link K2V} {@linkplain K2V#query() query transaction}.
 */
public abstract class Query implements Closeable {

  /**
   * root {@link Folder} at the time this transaction object was created
   */
  public final Folder root;
  
  protected Query(final Folder root) {
    this.root = root;
  }
  
  /**
   * Terminates the query.
   * <p>Call this method with code like:
   * <pre>{@code .
   *  final Query query = db.query();
   *  try {
   *    // Do query operations here.
   *    ...
   *  } finally {
   *    query.close();
   *  }
   * }</pre>
   */
  public abstract void close();

  /**
   * Finds a child {@link Entry}.
   * <p>
   * If the specified {@link Folder} is a saved result from a past {@link Query}
   * or {@link Update}, the return value might reflect that version of the
   * {@link Folder} or a subsequent update of it.
   * @param folder  {@link Folder} to search
   * @param key     {@link Entry} to search for
   * @return corresponding {@link Entry}, or {@link MissingEntry} if none
   */
  public abstract Entry find(Folder folder, byte[] key) throws IOException;
}
