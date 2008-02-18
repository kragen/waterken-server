// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.test.uri;

import org.waterken.uri.URI;

/**
 * Package test.
 */
final class
Main {

    private
    Main() {}

    static public void
    main(final String[] args) throws Exception {
        
        // test custom URIs
        if (args.length >= 1) {
            final String base = args[0];
            for (int i = 1; i != args.length; ++i) {
                System.out.println(URI.resolve(base, args[i]));
            }
        }
        
        testStandard();
    }
    
    static private void
    testStandard() {
        final String base = "http://a/b/c/d;p?q";

        // normal examples
        is(base, "g:h", "g:h");
        is(base, "g", "http://a/b/c/g");
        is(base, "./g", "http://a/b/c/g");
        is(base, "g/", "http://a/b/c/g/");
        is(base, "/g", "http://a/g");
        is(base, "//g", "http://g");
        is(base, "?y", "http://a/b/c/d;p?y");
        is(base, "?y#s", "http://a/b/c/d;p?y#s");
        is(base, "g?y", "http://a/b/c/g?y");
        is(base, "#s", "http://a/b/c/d;p?q#s");
        is(base, "g#s", "http://a/b/c/g#s");
        is(base, "g?y#s", "http://a/b/c/g?y#s");
        is(base, ";x", "http://a/b/c/;x");
        is(base, "g;x", "http://a/b/c/g;x");
        is(base, "g;x?y#s", "http://a/b/c/g;x?y#s");
        is(base, "", "http://a/b/c/d;p?q");
        is(base, ".", "http://a/b/c/");
        is(base, "./", "http://a/b/c/");
        is(base, "..", "http://a/b/");
        is(base, "../", "http://a/b/");
        is(base, "../g", "http://a/b/g");
        is(base, "../..", "http://a/");
        is(base, "../../", "http://a/");
        is(base, "../../g", "http://a/g");

        // abnormal examples
        is(base, "../../..", "http://a/");
        is(base, "../../../", "http://a/");
        is(base, "../../../..", "http://a/");
        is(base, "../../../../", "http://a/");
        is(base, "../../../../.", "http://a/");
        is(base, "../../../g", "http://a/g");
        is(base, "../../../../g", "http://a/g");
        is(base, "/./g", "http://a/g");
        is(base, "/../g", "http://a/g");
        is(base, "g.", "http://a/b/c/g.");
        is(base, ".g", "http://a/b/c/.g");
        is(base, "g..", "http://a/b/c/g..");
        is(base, "..g", "http://a/b/c/..g");
        is(base, "./../g", "http://a/b/g");
        is(base, "./g/.", "http://a/b/c/g/");
        is(base, "g/./h", "http://a/b/c/g/h");
        is(base, "g/../h", "http://a/b/c/h");
        is(base, "g;x=1/./y", "http://a/b/c/g;x=1/y");
        is(base, "g;x=1/../y", "http://a/b/c/y");
        is(base, "g?y/./x", "http://a/b/c/g?y/./x");
        is(base, "g?y/../x", "http://a/b/c/g?y/../x");
        is(base, "g#s/./x", "http://a/b/c/g#s/./x");
        is(base, "g#s/../x", "http://a/b/c/g#s/../x");
    }

    static private void
    is(final String base, final String relative, final String absolute) {
        final String resolved = URI.resolve(base, relative);
        if (!resolved.equals(absolute)) {
            throw new RuntimeException(resolved + " != " + absolute);
        }
    }
}
