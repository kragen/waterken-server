// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v.boxcar;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import org.k2v.Folder;
import org.k2v.K2V;
import org.k2v.Query;
import org.k2v.Update;

/**
 * A {@link K2V} that merges {@linkplain K2V#update updates}.
 */
public final class BoxcarK2V implements K2V {
  private   final     K2V base;
  protected final     Object cabooseLock = new Object();
  protected           Boxcar caboose = null;
  static final class Boxcar {
                int remaining = 10;
                int pending = 0;
    final       Semaphore updating = new Semaphore(1, true);
    final       CountDownLatch completed = new CountDownLatch(1);
    /** once */ Update update = null;
    /** once */ IOException failure = null;
  }
  private BoxcarK2V(final K2V base) {
    this.base = base;
  }
  static K2V make(final K2V base) { return new BoxcarK2V(base); }

  public void   close()  throws IOException { base.close(); }
  public Query  query()  throws IOException { return base.query(); }
  public Update update() throws IOException {
    final Boxcar m;
    synchronized (cabooseLock) {
      if (null == caboose) {
        m = caboose = new Boxcar();
      } else {
        m = caboose;
      }
      m.remaining -= 1;
      if (0 == m.remaining) {
        caboose = null;
      }
      m.pending += 1;
    }
    try {
      m.updating.acquire();
    } catch (final InterruptedException e) {
      throw (IOException)new InterruptedIOException().initCause(e);
    } finally {
      synchronized (cabooseLock) {
        m.pending -= 1;
      }
    }
    if (null == m.update) {
      boolean done = false;
      try {
        m.update = base.update();
        done = true;
      } finally {
        if (!done) {
          m.updating.release();
        }
      }
    }
    return new Update() {
      private boolean closed = false;
      private boolean master = false;
      private boolean release() {
        if (!closed) {
          closed = true;
          synchronized (cabooseLock) {
            master = 0 == m.pending; 
            if (master) {
              if (m == caboose) {
                caboose = null;
              }
            }
          }
          m.updating.release();
        }
        return master;
      }

      public void close() { if (release()) { m.update.close(); } }
      public void commit() throws IOException {
        if (closed) { throw new IOException(); }
        if (release()) {
          boolean done = false;
          try {
            m.update.commit();
            done = true;
          } finally {
            if (!done) {
              m.failure = new IOException();
            }
            m.completed.countDown();
          }
        } else {
          try {
            m.completed.await();
          } catch (final InterruptedException e) {
            throw (IOException)new InterruptedIOException().initCause(e);
          }
        }
        if (null != m.failure) { throw m.failure; }
      }
      public OutputStream open(final Folder folder,
                               final byte[] key) throws IOException {
        if (closed) { throw new IOException(); }
        return m.update.open(folder, key);
      }
      public Folder touch(final Folder folder) throws IOException {
        if (closed) { throw new IOException(); }
        return m.update.touch(folder);
      }
      public Folder nest(final Folder folder,
                         final byte[] key) throws IOException {
        if (closed) { throw new IOException(); }
        return m.update.nest(folder, key);
      }
    };
  }
}
