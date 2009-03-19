// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.waterken.archive.Archive;
import org.waterken.io.Stream;

/**
 * Command to extract entries from an archive.
 */
public final class
Extract {
    private Extract() {}
    
    /**
     * command line arguments format: {@value}
     */
    static public final String syntax = "'archive filename' 'entry name'*";

    /**
     * Extracts entries in an archive.
     * @param args   {@link #syntax}
     * @throws IOException  any I/O problem
     */
    static public void
    main(final String[] args) throws IOException {
        if (0 == args.length) {
            System.err.println("expected arguments: " + syntax);
            System.exit(-1);
            return;
        }
        final N2V n2v = N2V.open(new File(args[0]));
        if (1 == args.length) {
            extractAll(n2v);
        } else {
            final String[] selected = new String[args.length - 1];
            System.arraycopy(args, 1, selected, 0, selected.length);
            extractSelected(n2v, selected);
        }
    }
    
    /**
     * Extracts all entries in an archive.
     * @param archive   archive to read
     * @throws IOException  any I/O problem
     */
    static public void
    extractAll(final N2V archive) throws IOException {
        for (final Archive.Entry entry : archive) { reify(entry); }
    }
    
    /**
     * Extracts selected entries in an archive.
     * @param archive   archive to read
     * @param selected  each filename to extract
     * @throws IOException  any I/O problem
     */
    static public void
    extractSelected(final N2V archive,
                    final String... selected) throws IOException {
        for (final String filename : selected) {
            final Archive.Entry entry = archive.find(filename);
            if (null == entry) { throw new FileNotFoundException(filename); }
            reify(entry);
        }
    }
    
    static private void
    reify(final Archive.Entry entry) throws IOException {
        final File file = new File(entry.getPath());
        file.delete();
        if (entry.isDirectory()) {
            file.mkdirs();
        } else {
            final File parent = file.getParentFile();
            if (null != parent) { parent.mkdirs(); }
            if (!file.createNewFile()) { throw new IOException(); }
            if (0 != entry.getLength()) {
                final FileOutputStream out = new FileOutputStream(file);
                final InputStream in = entry.open();
                Stream.copy(in, out);
                in.close();
                out.flush();
                out.close();
            }
        }
    }
}
