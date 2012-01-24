// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v.trie;

import java.io.File;
import java.security.SecureRandom;

import org.k2v.test.Testing;

final class Test {
  private Test() {}

  /**
   * @param args
   */
  static public void main(String[] args) throws Exception {
    Testing.clean(new File("").getCanonicalFile());
    final Trie db = Trie.create(new File("test"+Trie.Ext), new SecureRandom());
    final long startTime = System.currentTimeMillis();
    Testing.test(db);
    db.compact(new File("compacted" + Trie.Ext)).close();
    db.close();
    final long endTime = System.currentTimeMillis();
    System.out.println((endTime - startTime) + " ms");
    Trie.open(new File("test" + Trie.Ext)).close();
    Trie.open(new File("compacted" + Trie.Ext)).close();
  }
}
