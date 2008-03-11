// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.PrintStream;

import org.waterken.vat.Vat;
import org.waterken.vat.Root;
import org.waterken.vat.Transaction;

/**
 * Touches all the mutable objects in a vat.
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

        touch(new File(args[0]));
    }
    
    static private void
    touch(final File folder) throws Exception {
        folder.listFiles(new FileFilter() {
            public boolean
            accept(final File file) {
                if (file.isDirectory()) {
                    try { touch(file); } catch (final Exception e) {}
                }
                return false;
            }
        });
        new JODB(folder, null).process(Vat.change, new Transaction<Void>() {
            public Void
            run(final Root local) throws Exception {
                folder.list(new FilenameFilter() {
                    public boolean
                    accept(final File dir, final String name) {
                        if (!name.endsWith(JODB.ext)) { return false; }
                        try {
                            local.fetch(null, name.substring(
                               0, name.length() - JODB.ext.length()));
                        } catch (final Exception e) {} // ignore problem object
                        return false;
                    }
                });
                return null;
            }
        });
    }
}
