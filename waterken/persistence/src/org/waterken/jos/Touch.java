// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintStream;

import org.waterken.model.Heap;
import org.waterken.model.Model;
import org.waterken.model.Root;
import org.waterken.model.Transaction;

/**
 * Touches all the mutable objects in a database.
 */
final class
Touch {
    
    private
    Touch() {}

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
            final PrintStream log = System.err;
            log.println("Touches all mutable objects in a persistence folder.");
            log.println("use: java -jar touch.jar <folder-path>");
            System.exit(-1);
            return;
        }

        final File folder = new File(args[0]);
        ((JODB)JODB.connect(folder)).process(Model.change,
                                             new Transaction<Void>() {
            public Void
            run(final Root local) throws Exception {
                final Heap heap = (Heap)local.fetch(null, Root.heap);
                folder.list(new FilenameFilter() {
                    public boolean
                    accept(final File dir, final String name) {
                        if (!name.startsWith(JODB.prefix)) { return false; }
                        if (!name.endsWith(JODB.ext)) { return false; }
                        heap.reference(Long.parseLong(name.substring(
                            JODB.prefix.length(),
                            name.length() - JODB.ext.length()), 16));
                        return false;
                    }
                });
                return null;
            }
        });
    }
}
