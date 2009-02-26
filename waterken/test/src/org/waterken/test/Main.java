// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.test;

/**
 * Runs all tests.
 */
public final class
Main {
    private Main() {}

    /**
     * Runs all tests.
     */
    static public void
    main(final String[] args) throws Exception {
        org.waterken.test.uri.Main.main(args);
        org.waterken.all.All.main(args);
        org.waterken.factorial.FactorialN.main(args);
    }
}
