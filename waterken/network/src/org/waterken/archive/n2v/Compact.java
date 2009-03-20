// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.waterken.io.Stream;

/**
 * Command to create an archive of named files.
 */
public final class
Compact {
    private Compact() {}
    
    /**
     * command line arguments format: {@value}
     */
    static public final String syntax = "'archive filename or - for stdout'" +
                                        " 'file or directory path'*";

    /**
     * Compacts named files in an archive.
     * @param args  {@link #syntax}
     * @throws IOException  any I/O problem
     */
    static public void
    main(final String[] args) throws IOException {
        if (0 == args.length) {
            System.err.println("expected arguments: " + syntax);
            System.exit(-1);
            return;
        }
        final OutputStream out;
        if ("-".equals(args[0])) {
            out = System.out;
        } else {
            final File file = new File(args[0]);
            if (!file.createNewFile()) { throw new IOException(); }
            out = new FileOutputStream(file);
        }
        final N2VOutput n2v = new N2VOutput(out);
        for (int i = 1; i != args.length; ++i) {
            append(n2v, new File(args[i]));
        }
        n2v.finish();
        out.flush();
        out.close();
    }
    
    static private void
    append(final N2VOutput n2v, final File file) throws IOException {
        if (file.isFile()) {
            final FileInputStream in = new FileInputStream(file);
            final OutputStream out = n2v.append(path(file));
            Stream.copy(in, out);
            in.close();
            out.flush();
            out.close();
        } else if (file.isDirectory()) {
            n2v.append(path(file) + '/').close();
            file.listFiles(new FileFilter() {
                public boolean
                accept(final File child) {
                    try {
                        append(n2v, child);
                    } catch (final IOException e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                }
            });
        }
    }
    
    static private String
    path(final File f) { return f.getPath().replace(File.separatorChar, '/'); }
}
