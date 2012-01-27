// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v.gentrie;

import java.io.EOFException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.SecureRandom;
import java.util.ArrayList;

import org.k2v.Value;
import org.k2v.Folder;
import org.k2v.K2V;
import org.k2v.Query;
import org.k2v.Update;
import org.k2v.trie.Trie;

/**
 * A {@link Trie} with automatic, generational
 * {@linkplain Trie#compact compaction}.
 */
public final class GenTrie implements K2V {

  protected final    File dir;
  protected volatile Thread compacting = null;
  protected volatile Trie[] generations;
  
  private GenTrie(final File dir, final Trie[] generations) {
    this.dir = dir;
    this.generations = generations;
  }
  
  /**
   * Creates a new {@link GenTrie} folder.
   * @param dir   folder location
   * @param prng  strong random number generator
   * @return empty {@link GenTrie} that MUST be {@linkplain #update() updated}
   */
  static public K2V create(final File dir,
                           final SecureRandom prng) throws IOException {
    final String[] unexpected = dir.list(new FilenameFilter() {
      public boolean accept(final File _, final String name) {
        return name.endsWith(Trie.Ext);
      }
    });
    if (0 != unexpected.length) {
      throw new IOException("unexpected data file: " + unexpected[0]);
    }
    return new GenTrie(dir, new Trie[] {
        Trie.create(new File(dir, "A1" + Trie.Ext), prng)
      });
  }
  
  /**
   * Opens an existing {@link GenTrie} folder.
   * @param dir   folder location
   * @return {@link GenTrie} ready for use
   * @throws EOFException {@code dir} has invalid format
   */
  static public K2V open(final File dir) throws IOException {
    final String[] youngFatOldDead = new String[4];
    final String[] unexpected = dir.list(new FilenameFilter() {
      public boolean accept(final File _, final String name) {
        if (!name.endsWith(Trie.Ext)) { return false; }
        if (name.startsWith("A")) {
          if (null == youngFatOldDead[0]) {
            youngFatOldDead[0] = name;
          } else if (null == youngFatOldDead[1]) {
            if (id(name) - id(youngFatOldDead[0]) > 0) {
              youngFatOldDead[1] = youngFatOldDead[0];
              youngFatOldDead[0] = name;
            } else {
              youngFatOldDead[1] = name;
            }
          } else {
            return true;
          }
        } else if (name.startsWith("B")) {
          if (null == youngFatOldDead[2]) {
            youngFatOldDead[2] = name;
          } else if (null == youngFatOldDead[3]) {
            if (id(name) - id(youngFatOldDead[2]) > 0) {
              youngFatOldDead[3] = name;
            } else {
              youngFatOldDead[3] = youngFatOldDead[2];
              youngFatOldDead[2] = name;
            }
          } else {
            return true;
          }
        } else {
          return true;
        }
        return false;
      }
    });
    if (0 != unexpected.length) {
      throw new IOException("unexpected data file " + unexpected[0]);
    }
    if (null == youngFatOldDead[0]) {
      throw new IOException("missing data file");
    }
    if (null != youngFatOldDead[3]) {
      new File(dir, youngFatOldDead[3]).delete();
    }
    if (null == youngFatOldDead[1]) {
      if (null == youngFatOldDead[2]) {
        return new GenTrie(dir,
            new Trie[] { Trie.open(new File(dir, youngFatOldDead[0])) });
      } else {
        final Trie old;
        try {
          old = Trie.open(new File(dir, youngFatOldDead[2])); 
        } catch (final EOFException e) {
          return new GenTrie(dir,
              new Trie[] { Trie.open(new File(dir, youngFatOldDead[0])) });
        }
        try {
          final Trie young = Trie.open(new File(dir, youngFatOldDead[0])); 
          return new GenTrie(dir, new Trie[] { young, old });
        } catch (final EOFException e) {
          final File file = new File(dir, youngFatOldDead[0]); 
          file.delete();
          return new GenTrie(dir, new Trie[] { old });
        }
      }
    } else {
      if (null == youngFatOldDead[2]) {
        final Trie young;
        try {
          young = Trie.open(new File(dir, youngFatOldDead[0])); 
        } catch (final EOFException e) {
          return new GenTrie(dir,
              new Trie[] { Trie.open(new File(dir, youngFatOldDead[1])) });
        }
        final GenTrie r = new GenTrie(dir, new Trie[] {
            young,
            Trie.open(new File(dir, youngFatOldDead[1]))
          });
        r.compact(young, r.generations[1], null);
        return r;
      } else {
        final Trie young;
        try {
          young = Trie.open(new File(dir, youngFatOldDead[0])); 
        } catch (final EOFException e) {
          return new GenTrie(dir, new Trie[] {
              Trie.open(new File(dir, youngFatOldDead[1])),
              Trie.open(new File(dir, youngFatOldDead[2]))
          });
        }
        final GenTrie r = new GenTrie(dir, new Trie[] {
            young,
            Trie.open(new File(dir, youngFatOldDead[1])),
            Trie.open(new File(dir, youngFatOldDead[2]))
          });
        r.compact(young, r.generations[1], r.generations[2]);
        return r;
      }
    }
  }
  
  public void close() throws IOException {
    final Thread thread = compacting;
    if (null != thread) {
      try {
        thread.join();
      } catch (final InterruptedException e) {
        throw (IOException)new InterruptedIOException().initCause(e);
      }
    }
    final Trie[] current = generations;
    for (final Trie generation : current) { generation.close(); }
  }

  public Query query() throws IOException {
    final Trie[] current = generations;
    final Query[] base = new Query[current.length];
    for (int i = base.length; 0 != i--;) {
      base[i] = current[i].query();
    }
    return new Query(base[0].root) {
      public void close() { for (final Query x : base) { x.close(); } }
      public Value
      find(final Folder folder, final byte[] key) throws IOException {
        final long version = ((Trie.Folder)folder).getVersion();
        for (int i = 0; i != current.length; ++i) {
          if (version >= current[i].firstVersion) {
            final Value r = base[i].find(folder, key);
            if (!(r instanceof Trie.Null)) { return r; }
            if (((Trie.Null)r).absolute)   { return r; }
          }
        }
        return new Trie.Null(false);
      }
    };
  }

  public Update update() throws IOException {
    while (true) {
      final Trie head = generations[0];
      final Trie.Update base = (Trie.Update)head.update();
      if (head == generations[0]) {
        if (head.getLoadFactor() > 0.1F || null != compacting) { return base; }
        try {
          final Trie young = base.spawn(reuse(new File(dir, turn(head.name))));
          final Trie old = 1 == generations.length ? null : generations[1];
          compact(young, head, old);
          generations = null == old ? new Trie[] { young, head } :
                                      new Trie[] { young, head, old };
        } finally {
          base.close();
        }
      } else {
        base.close();
      }
    }
  }
  
  private void compact(final Trie young, final Trie fat, final Trie old) {
    compacting = new Thread(new Runnable() {
      public void run() {
        try {
          final ArrayList<Trie> dead = new ArrayList<Trie>(2);
          dead.add(fat);
          Trie target = old;
          if (null == target) {
            target = fat.compact(reuse(new File(dir, "B1" + Trie.Ext)));
          } else {
            target.merge(fat);
            if (target.getLoadFactor() < 0.5F) {
              dead.add(target);
              target = target.compact(reuse(new File(dir, turn(target.name))));
            }
          }
          generations = new Trie[] { young, target };
          for (final Trie x : dead) {
            try { x.close(); } catch (final IOException e) {}
          }
          for (final Trie x : dead) { delete(new File(dir, x.name)); }
        } catch (final IOException e) {
          throw new RuntimeException(e);
        } finally {
          compacting = null;
        }
      }
    });
    compacting.start();
  }
  
  static protected String turn(final String prior) {
    return prior.substring(0, 1) + (id(prior) + 1) + Trie.Ext;
  }
  
  static protected long id(final String name) {
    return Long.parseLong(name.substring(1, name.length() - Trie.Ext.length()));
  }
  
  static protected void delete(final File file) {
    while (!file.delete()) {
      try { Thread.sleep(1000); } catch (final InterruptedException e) {}
    }
  }
  
  static protected File reuse(final File file) {
    if (file.isFile()) { delete(file); }
    return file;
  }
}
