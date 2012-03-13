// Copyright 2011 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.k2v.boxcar;

import java.io.File;
import java.security.SecureRandom;

import org.k2v.K2V;
import org.k2v.gentrie.GenTrie;
import org.k2v.test.Testing;

final class Test {
  private Test() {}

  /**
   * @param args
   */
  static public void main(String[] args) throws Exception {
    final File dir = new File("").getCanonicalFile();
    Testing.clean(dir);
    final K2V db = BoxcarK2V.make(GenTrie.create(dir, new SecureRandom()));
    final long startTime = System.currentTimeMillis();
    Testing.test(db);
    db.close();
    final long endTime = System.currentTimeMillis();
    System.out.println((endTime - startTime) + " ms");
  }
}
