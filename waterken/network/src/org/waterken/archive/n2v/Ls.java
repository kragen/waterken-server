// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.File;
import java.io.IOException;

import org.waterken.archive.Archive;

/**
 * Command to list entries in an archive.
 */
public final class
Ls {
    private Ls() {}
    
    /**
     * command line arguments format: {@value}
     */
    static public final String syntax = "'archive filename' 'entry name'*";

    /**
     * Lists entries in an archive.
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
        final Archive archive = new N2V(new File(args[0]));
        if (1 == args.length) {
            listAll(archive);
        } else {
            final String[] selected = new String[args.length - 1];
            System.arraycopy(args, 1, selected, 0, selected.length);
            listSelected(archive, selected);
        }
    }
    
    /**
     * Lists all entries in an archive.
     * @param archive   archive to read
     * @throws IOException  any I/O problem
     */
    static public void
    listAll(final Archive archive) throws IOException {
        for (final Archive.Entry entry : archive) { list(entry); }
    }
    
    /**
     * Lists selected entries in an archive.
     * @param archive   archive to read
     * @param selected  each filename
     * @throws IOException  any I/O problem
     */
    static public void
    listSelected(final Archive archive,
                 final String... selected) throws IOException {
        for (final String filename : selected) {
            final Archive.Entry entry = archive.find(filename);
            if (null != entry) { list(entry); }
        }
    }
    
    static private void
    list(final Archive.Entry entry) {
        System.out.println(entry.getName());
    }
}
