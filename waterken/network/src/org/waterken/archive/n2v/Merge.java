// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

/**
 * Command to merge named archive files.
 */
public final class
Merge {
    private Merge() {}
    
    /**
     * command line arguments format: {@value}
     */
    static public final String syntax = "'output archive filename'" +
                                        " 'filename of archive to merge'*";

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
        final File file = new File(args[0]);
        if (!file.createNewFile()) { throw new IOException(); }
        final FileChannel out = new FileOutputStream(file).getChannel();
        final ArrayList<N2V> versions = new ArrayList<N2V>(args.length - 1);
        for (int i = 1; i != args.length; ++i) {
            versions.add(new N2V(new File(args[i])));
        }
        N2V.merge(out, versions);
        out.force(true);
        out.close();
    }
}
