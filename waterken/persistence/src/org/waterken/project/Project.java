// Copyright 2007-2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.project;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.joe_e.file.Filesystem;
import org.ref_send.promise.eventual.Loop;
import org.waterken.cache.Cache;

/**
 * A ClassLoader with a timestamp.
 */
public final class
Project extends URLClassLoader {
    
    /**
     * project name
     */
    public final String name;
    
    /**
     * timestamp of the newest class file
     */
    public final long timestamp;

    /**
     * Constructs an instance.
     * @param name      project name
     * @param jar       class file folder
     * @param parent    parent class loader
     * @throws IOException  problem accessing classes
     */
    private
    Project(final String name,
            final File jar, final ClassLoader parent) throws IOException {
        super(new URL[] { jar.toURI().toURL() }, parent);
        this.name = name;
        timestamp = findNewest(0L, jar);
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

    /**
     * home folder
     */
    static public  final File home;
    static private final File bins;
    static private final String bin;
    static {
        try {
            home = new File(System.getProperty(
                "waterken.home", "")).getCanonicalFile();
            bins = new File(home, System.getProperty(
                "waterken.code", "")).getCanonicalFile();
            bin = System.getProperty(
                "waterken.bin", File.separator + "bin" + File.separator);
        } catch (final IOException e) { throw new Error(e.getMessage(), e); }
    }

    static private final Cache<String,ClassLoader> jars = Cache.make();
    static private       ClassLoader shared = Loop.class.getClassLoader();
    static {
        try {
            shared = connect("shared");
        } catch (final Exception e) { throw new Error(e.getMessage(), e); }
    }

    /**
     * Gets the named classloader.
     * @param project   project name
     */
    static public ClassLoader
    connect(final String project) throws Exception {
        if (null == project || "".equals(project)) { return shared; }

        synchronized (jars) {
            ClassLoader r = jars.fetch(null, project);
            if (null == r) {
                // assume a safe value has been configured for waterken.bin,
                // and so only the project name need be vetted
                Filesystem.checkName(project);

                final File jar = new File(bins, project + ".jar");
                r = new Project(project,
                                jar.isFile() ? jar : new File(bins,project+bin),
                                shared);
                jars.put(project, r);
            }
            return r;
        }
    }
}
