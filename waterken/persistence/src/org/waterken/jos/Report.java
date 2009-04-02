// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.DataInputStream;
import java.io.File;
import java.io.ObjectStreamConstants;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

import org.waterken.archive.Archive;
import org.waterken.archive.n2v.N2V;

/**
 * Produce a report on the contents of an archive.
 */
final class
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
    main(final String[] args) throws Exception {
        final PrintStream out = System.out;
        if (0 == args.length) {
            out.println("Reports on the content of an archive version.");
            out.println("use: java -jar report.jar <file-path>");
            System.exit(-1);
            return;
        }

        final File file = new File(args[0]);
        if (!file.getName().endsWith(".n2v")) { throw new Exception(); }
        final Archive archive = N2V.open(file);
        report(out, archive);
        out.flush();
        out.close();
        archive.close();
    }

    static private final int minFilenameWidth = 26 + JODB.ext.length();

    static private final class
    Total {
        final String typename;
        int files;
        long bytes;

        Total(final String typename) {
            this.typename = typename;
        }
    }
    
    static private void
    report(final PrintStream out, final Archive archive) throws Exception {
        final HashMap<String,Total> total = new HashMap<String,Total>();
        out.println("--- Files ( filename, length, typename) ---");
        for (final Archive.Entry entry : archive) {
            final String typename = log(out, entry);

            // Keep track of totals.
            Total t = total.get(typename);
            if (null == t) { total.put(typename, t = new Total(typename)); }
            t.files += 1;
            t.bytes += entry.getLength();
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
    
    static private String
    log(final PrintStream out, final Archive.Entry entry) {
        out.print(entry.getName());
        for (int n = minFilenameWidth - entry.getName().length(); 0 < n--;) {
            out.print(' ');
        }
        out.print('\t');
        out.print(entry.getLength());
        out.print('\t');

        // Determine the type of object stored in the file.
        String typename;
        try {
            final DataInputStream data = new DataInputStream(entry.open());
            final int[] magic= new int[] { 0xAC, 0xED, 0x00, 0x05 };
            for (int i = 0; i != magic.length; ++i) {
                if (data.read() != magic[i]) { throw new Exception(); }
            }
            switch (data.read()) {
            case ObjectStreamConstants.TC_OBJECT: {
                switch (data.read()) {
                case ObjectStreamConstants.TC_CLASSDESC: {
                    typename= data.readUTF();
                }
                break;
                case ObjectStreamConstants.TC_PROXYCLASSDESC: {
                    typename = "proxy"; // TODO: extract implemented type
                }
                break;
                default: throw new Exception();
                }
            }
            break;
            case ObjectStreamConstants.TC_ARRAY: { typename = "array"; }
            break;
            case ObjectStreamConstants.TC_STRING: { typename = "string"; }
            break;
            case ObjectStreamConstants.TC_LONGSTRING: {typename= "long string";}
            break;
            case ObjectStreamConstants.TC_NULL: { typename = "null"; }
            break;
            default: throw new Exception();
            }
            data.close();
        } catch (final Exception e) {
            typename = "! " + e.getClass().getName();
        }
        out.println(typename);
        return typename;
    }
}
