// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.joe_e.file.Filesystem;

/**
 * Produce a report on file usage by a persistence folder.
 */
final class
Report {

    private static final class
    Total {
        final String typename;
        int files;
        long bytes;

        Total(final String typename) {
            this.typename = typename;
        }
    }

    /**
     * The command line arguments are:
     * <ol>
     *  <li>path to the persistence folder</li>
     * </ol>
     * @param args  argument string
     */
    static public void
    main(final String[] args) throws Exception {
        if (0 == args.length) {
            System.err.println("use: java -jar report.jar <folder>");
            System.exit(-1);
            return;
        }

        final File folder = new File(args[0]);
        final PrintStream out = System.out;
        final HashMap<String,Total> total = new HashMap<String,Total>();

        out.println("--- Files ( filename, length, typename) ---");
        final ClassLoader code = JODB.application(folder);
        for (final File file : folder.listFiles()) {
            if (!file.getName().endsWith(JODB.ext)) { continue; }
            out.print(file.getName());
            out.print('\t');
            out.print(file.length());
            out.print('\t');

            // Determine the type of object stored in the file.
            String typename;
            try {
                final Class[] type = { Void.class };
                final SubstitutionStream in = new SubstitutionStream(true,
                        code, Filesystem.read(file)) {
                    protected Object
                    resolveObject(Object x) throws IOException {
                        if (x instanceof Wrapper) {
                            type[0] = x.getClass();
                            x = null;
                        }
                        return x;
                    }
                };
                final Object x = in.readObject();
                in.close();
                if (null != x) { type[0] = x.getClass(); }
                typename = type[0].getName();
            } catch (final Exception e) {
                typename = "<broken>";
            }
            out.print(typename);
            out.println();

            // Keep track of totals.
            Total t = total.get(typename);
            if (null == t) { total.put(typename, t = new Total(typename)); }
            t.files += 1;
            t.bytes += file.length();
        }

        out.println();
        out.println("--- Totals ( files, bytes, typename) ---");
        final Total[] sum = total.values().toArray(new Total[total.size()]);
        Arrays.sort(sum, new Comparator<Total>() {
            public int
            compare(final Total a, final Total b) {
                return a.files == b.files
                    ? (a.bytes == b.bytes
                        ? a.typename.compareTo(b.typename)
                        : (a.bytes > b.bytes ? -1 : 1))
                    : (a.files > b.files ? -1 : 1);
            }
        });
        int files = 0;
        long bytes = 0L;
        for (final Total t : sum) {
            out.print(t.files);
            out.print('\t');
            out.print(t.bytes);
            out.print('\t');
            out.print(t.typename);
            out.println();

            files += t.files;
            bytes += t.bytes;
        }

        out.println();
        out.println("--- Total ---");
        out.println("files:\t" + files);
        out.println("bytes:\t" + bytes);
        out.println("types:\t" + sum.length);
    }
}
