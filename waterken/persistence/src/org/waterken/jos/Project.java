// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author tyler
 *
 */
final class
Project extends URLClassLoader {
    
    protected final long timestamp;

    Project(final File bin) throws IOException {
        super(new URL[] { bin.toURI().toURL() },
              Project.class.getClassLoader());
        if (!bin.canRead()) { throw new IOException("no classes"); }
        
        timestamp = findNewest(0L, bin);
    }
    
    static private long
    findNewest(long max, final File root) {
        if (root.isDirectory()) {
            for (final File f : root.listFiles()) {
                max = findNewest(max, f);
            }
        } else {
            max = Math.max(max, root.lastModified());
        }
        return max;
    }
}
