// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.waterken.archive.Archive;
import org.waterken.archive.n2v.N2V;

/**
 * Reports on the content of an archive.
 */
/* package */ final class
Report {
    private Report() {}

    /**
     * The command line arguments are:
     * <ol>
     *  <li>path to .n2v format file</li>
     * </ol>
     * @param args  argument string
     */
    static public void
    main(final String[] args) throws IOException {
        final PrintStream stdout = System.out;
        if (0 == args.length) {
            stdout.println("Reports on the content of an archive.");
            stdout.println("use: java -jar report.jar <file-path>");
            System.exit(-1);
            return;
        }
        final File file = new File(args[0]);
        
        final Archive archive = N2V.open(file);
        report(stdout, archive);
        stdout.flush();
        stdout.close();
        archive.close();
    }

    static private final int minNameWidth = 26 + JODB.ext.length();

    static private final class
    Total {
        final String typename;
        int entries;
        long bytes;

        Total(final String typename) {
            this.typename = typename;
        }
    }
    
    static private void
    report(final PrintStream stdout, final Archive archive) throws IOException {
        final HashMap<String,Total> total = new HashMap<String,Total>();
        stdout.println("--- Entries ( name, length, typename) ---");
        for (final Archive.Entry entry : archive) {
            stdout.print(entry.getName());
            for (int n = minNameWidth - entry.getName().length(); 0 < n--;) {
                stdout.print(' ');
            }
            stdout.print('\t');
            stdout.print(entry.getLength());
            stdout.print('\t');
            final String typename = JODB.identify(entry.open());
            stdout.println(typename);

            // keep track of totals
            Total t = total.get(typename);
            if (null == t) { total.put(typename, t = new Total(typename)); }
            t.entries += 1;
            t.bytes += entry.getLength();
        }
        stdout.println();
        stdout.println("--- Totals ( entries, bytes, typename) ---");
        final Total[] sum = total.values().toArray(new Total[total.size()]);
        Arrays.sort(sum, new Comparator<Total>() {
            public int
            compare(final Total a, final Total b) {
                return a.entries == b.entries
                    ? (a.bytes == b.bytes
                        ? a.typename.compareTo(b.typename)
                        : (a.bytes > b.bytes ? -1 : 1))
                    : (a.entries > b.entries ? -1 : 1);
            }
        });
        int entries = 0;
        long bytes = 0L;
        for (final Total t : sum) {
            stdout.print(t.entries);
            stdout.print('\t');
            stdout.print(t.bytes);
            stdout.print('\t');
            stdout.print(t.typename);
            stdout.println();

            entries += t.entries;
            bytes += t.bytes;
        }

        stdout.println();
        stdout.println("--- Total ---");
        stdout.println("entries:\t" + entries);
        stdout.println("bytes:\t" + bytes);
        stdout.println("types:\t" + sum.length);
    }
}
