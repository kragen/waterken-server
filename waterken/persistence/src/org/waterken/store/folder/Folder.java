// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store.folder;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.joe_e.var.Milestone;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.store.Store;
import org.waterken.store.StoreMaker;
import org.waterken.store.Update;

/**
 * A file system based <code>byte</code> storage implementation.
 */
public final class
Folder extends Struct implements StoreMaker, Serializable {
    static private final long serialVersionUID = 1L;

    private final Receiver<Long> sleep;
    
    /**
     * Constructs an instance.
     */
    public
    Folder(final Receiver<Long> sleep) {
        this.sleep = sleep;
    }
    
    // org.waterken.store.StoreMaker interface
    
    public Store
    run(final Receiver<Promise<?>> background,
        final File parent, final File dir) {
        final File pending = Filesystem.file(dir, ".pending");
        final File committed = Filesystem.file(dir, ".committed");
        return new Store() {
            private boolean active;   // Is there an update in progress?
            
            public void
            clean() throws IOException {
                final File tmp = Filesystem.file(parent,".dead."+dir.getName());
                if (dir.isDirectory()) {
                    rename(dir, tmp);
                }
                if (tmp.isDirectory()) {
                    deleteRecursively(tmp);
                }
            }
            
            public synchronized Update
            update() throws IOException {
                while (active) {
                    try {
                        wait();
                    } catch (final InterruptedException e) {
                        throw new InterruptedIOException();
                    }
                }
                
                if (!dir.isDirectory()) { throw new FileNotFoundException(); }
                if (pending.isDirectory()) {
                    if (committed.isDirectory()) {
                        deleteRecursively(committed);
                    }
                    deleteRecursively(pending);
                } else if (committed.isDirectory()) {
                    renameAll(committed, dir);
                }
                active = true;
                final Milestone<Boolean> done = Milestone.plan();
                final Milestone<Boolean> nested = Milestone.plan();
                final Milestone<Boolean> written = Milestone.plan();
                final Object lock = this;
                return new Update() {
                    
                    public void
                    close() {
                        done.mark(true);
                        synchronized (lock) {
                            active = false;
                            lock.notify();
                        }
                    }

                    public InputStream
                    read(final String filename) throws IOException {
                        if (done.is()) { throw new AssertionError(); }
                        try {
                            return Filesystem.read(
                                    Filesystem.file(dir, filename));
                        } catch (final InvalidFilenameException e) {
                            throw new FileNotFoundException();
                        }
                    }
                    
                    private boolean isWriting = false;
                    
                    public OutputStream
                    write(final String filename) throws IOException {
                        if (done.is()) { throw new AssertionError(); }
                        if (isWriting) { throw new AssertionError(); }
                        if (written.mark(true) && !nested.is()){mkdir(pending);}
                        if(!ok(filename)){throw new InvalidFilenameException();}
                        final OutputStream out =
                            writeNew(Filesystem.file(pending, filename));
                        isWriting = true;
                        return new OutputStream() {
                            
                            public void
                            write(int b) throws IOException { out.write(b); }

                            public void
                            write(byte[] b) throws IOException { out.write(b); }

                            public void
                            write(byte[] b,int off,int len) throws IOException {
                                out.write(b, off, len);
                            }

                            public void
                            flush() throws IOException { out.flush(); }

                            public void
                            close() throws IOException {
                                isWriting = false;
                                out.close();
                            }
                        };
                    }
                    
                    public Store
                    nest(final String filename) throws IOException {
                        if (done.is()) { throw new AssertionError(); }
                        final String was = "." + filename + ".was";
                        if (filename.startsWith(".") || !ok(filename) ||
                            Filesystem.file(dir, was).isFile() ||
                            (nested.is() &&
                             Filesystem.file(pending, was).isFile())) {
                            throw new InvalidFilenameException();
                        }
                        if (nested.mark(true) && !written.is()){mkdir(pending);}
                        final File child = Filesystem.file(pending, filename);
                        mkdir(child);
                        writeNew(Filesystem.file(pending, was)).close();
                        return run(background, pending, child);
                    }
                    
                    public void
                    commit() throws IOException {
                        if (done.is()) { throw new AssertionError(); }
                        done.mark(true);
                        if (nested.is() || written.is()) {
                            while (true) {
                                try {
                                    rename(pending, committed);
                                    break;
                                } catch (final IOException e) {
                                    if (!pending.isDirectory()) { throw e; }
                                    sleep.run(100L);
                                }
                            }
                            renameAll(committed, dir);
                        } else {
                            if (!dir.isDirectory()) {
                                throw new FileNotFoundException();
                            }
                        }
                    }
                };
            }
        };
    }
    
    static private boolean
    ok(final String name)  {
        if (0 == name.length()) { return false; }
        for (int i = name.length(); i-- != 0;) {
            if (disallowed.indexOf(name.charAt(i)) != -1) { return false; }
        }
        return true;
    }
    
    // the rest is static implementation helpers for file I/O
    
    /**
     * Creates a file for writing.
     * @param file  file to create
     * @return opened output stream
     * @throws IOException    <code>file</code> could not be created
     */
    static private OutputStream
    writeNew(final File file) throws IOException {
        if (!file.createNewFile()) { throw new IOException(); }
        return new SynchedFileOutputStream(file);
    }
    
    /**
     * Makes a new directory.
     * @param dir   directory to make
     * @throws IOException  <code>dir</code> could not be created
     */
    static private void
    mkdir(final File dir) throws IOException {
        if (!dir.mkdir()) { throw new IOException(); }
    }
    
    static private void
    rename(final File from, final File to) throws IOException {
        if (!from.renameTo(to)) { throw new IOException(); }
    }
    
    /**
     * Renames the children of a source folder to a destination folder.
     * @param from  source folder
     * @param to    destination folder
     * @throws IOException  any I/O problem
     */
    static private void
    renameAll(final File from, final File to) throws IOException {
        final File[] failed = from.listFiles(new FileFilter() {
            public boolean
            accept(final File child) {
                final File dst = Filesystem.file(to, child.getName());
                dst.delete();
                return !child.renameTo(dst);
            }
        });
        if (null == failed || 0 != failed.length) { throw new IOException(); }
        if (!from.delete()) { throw new IOException(); }
    }

    /**
     * Recursively deletes a filesystem entry.
     * @param file  entry to recursively delete
     * @throws IOException  any I/O problem
     */
    static private void
    deleteRecursively(final File file) throws IOException {
        if (file.isDirectory()) {
            if (0 != file.listFiles(new FileFilter() {
                public boolean
                accept(final File child) {
                    try {
                        deleteRecursively(child);
                    } catch (final IOException e) { return true; }
                    return false;
                }
            }).length) { throw new IOException(); }
        }
        if (!file.delete()) { throw new IOException(); }
    }
}