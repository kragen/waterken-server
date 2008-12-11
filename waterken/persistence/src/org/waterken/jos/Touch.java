// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;

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
        final PrintStream log = System.err;
        if (0 == args.length) {
            log.println("Touches all mutable objects in a persistence folder.");
            log.println("use: java -jar touch.jar <folder-path>");
            System.exit(-1);
            return;
        }

        touch(new File(args[0]), log);
    }
    
    static private void
    touch(final File dir, final PrintStream log) throws Exception {
        dir.listFiles(new FileFilter() {
            public boolean
            accept(final File file) {
                if (file.isDirectory()) {
                    try { touch(file, log); } catch (final Exception e) {}
                }
                return false;
            }
        });
        log.println(dir.getPath() + " ...");
        /* TODO
        final JODB db = (JODB)new JODBCache().connect(dir);
        db.process(new Transaction<Immutable>(Transaction.update) {
            public Immutable
            run(final Root local) throws Exception {
                dir.list(new FilenameFilter() {
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
        */
    }
}
