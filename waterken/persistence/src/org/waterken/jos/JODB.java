// Copyright 2002-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import static org.joe_e.file.Filesystem.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;

import org.joe_e.JoeE;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.file.Filesystem;
import org.joe_e.file.InvalidFilenameException;
import org.ref_send.list.List;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Rejected;
import org.ref_send.promise.eventual.Loop;
import org.ref_send.promise.eventual.Task;
import org.waterken.model.Creator;
import org.waterken.model.Effect;
import org.waterken.model.Heap;
import org.waterken.model.Model;
import org.waterken.model.ModelError;
import org.waterken.model.NoLabelReuse;
import org.waterken.model.ProhibitedModification;
import org.waterken.model.Root;
import org.waterken.model.Service;
import org.waterken.model.Transaction;
import org.waterken.thread.Concurrent;

/**
 * An object graph stored as a folder of Java Object Serialization files.
 */
public final class
JODB extends Model {

    /**
     * canonical persistence folder
     */
    private final File folder;
    
    /**
     * {@link Root#code}
     */
    private final ClassLoader code;

    /**
     * {@link Root#prng}
     */
    private final SecureRandom prng;

    private
    JODB(final File folder, final Loop<Service> service) throws Exception {
        super(service);
        this.folder = folder;

        code = application(folder);
        prng = new SecureRandom();
    }

    /**
     * file path to the directory containing all persistence folders
     */
    static public  final String dbDirName = "db";
    static private final File dbDir;
    static private final String dbDirPath;
    static {
        try {
            dbDir = new File(dbDirName).getCanonicalFile(); 
            dbDirPath = dbDir.getPath() + File.separator;
        } catch (final IOException e) {
            throw new ModelError(e);
        }
    }
    static private final HashMap<File,SoftReference<JODB>> live =
        new HashMap<File,SoftReference<JODB>>();

    static private final HashMap<String,SoftReference<ClassLoader>> jars =
        new HashMap<String,SoftReference<ClassLoader>>();
    
    /**
     * Gets the corresponding classloader for a persistence folder.
     * @param folder    canonical persistence folder
     */
    static @SuppressWarnings("unchecked") ClassLoader
    application(final File folder) throws Exception {
        
        // load the project name
        final File projectFile = file(folder, Root.project + ext);
        if (!projectFile.isFile()) { return JODB.class.getClassLoader(); }
        final String project;
        InputStream in = Filesystem.read(projectFile);
        try {
            final ObjectInputStream oin = new ObjectInputStream(in);
            in = oin;
            final SymbolicLink value = (SymbolicLink)oin.readObject();
            project = (String)value.target;
        } catch (final Exception e) {
            try { in.close(); } catch (final Exception e2) {}
            throw e;
        }
        in.close();
        if (null == project || "".equals(project)) {
            return JODB.class.getClassLoader();
        }

        // find the cached classloader
        synchronized (jars) {
            final SoftReference<ClassLoader> sr = jars.get(project);
            ClassLoader r = null != sr ? sr.get() : null;
            if (null == r) {
                final File bin = new File(
                    project + File.separator + "bin" + File.separator);
                if (!bin.isDirectory()) { throw new IOException("no classes"); }
                r = new URLClassLoader(new URL[] { bin.toURI().toURL() });
                jars.put(project, new SoftReference<ClassLoader>(r));
            }
            return r;
        }
    }

    protected void
    finalize() {
        synchronized (live) {
            live.remove(folder);
        }
        synchronized (jars) {
            final Iterator<SoftReference<ClassLoader>> i =
                jars.values().iterator();
            while (i.hasNext()) {
                if (null == i.next().get()) {
                    i.remove();
                }
            }
        }
    }

    /**
     * Opens an existing, but not yet open, model.
     * @param id        persistence folder
     * @param service   {@link #service}
     * @throws IOException  <code>folder</code> is not a persistence folder
     */
    static public Model
    open(final File id, final Loop<Service> service) throws Exception {
        final File folder = id.getCanonicalFile();
        synchronized (live) {
            if (null != live.get(folder)) { throw new AssertionError(); }
            return load(folder, service);
        }
    }

    static private final ThreadGroup threads = new ThreadGroup(dbDirName);
    
    /**
     * Connects to an existing model.
     * <p>
     * If the model is not yet {@link #open open}, it will be opened with a
     * {@link Concurrent#loop concurrent} {@link #service} loop.
     * </p>
     * @param id    persistence folder
     * @throws IOException  <code>folder</code> is not a persistence folder
     */
    static public Model
    connect(final File id) throws Exception {
        final File folder = id.getCanonicalFile();
        synchronized (live) {
            final SoftReference<JODB> sr = live.get(folder);
            if (null != sr) {
                final JODB r = sr.get();
                if (null != r) { return r; }
            }
            final Loop<Service> service = Concurrent.loop(threads,
                folder.getPath().substring(dbDir.getPath().length()));
            return load(folder, service);
        }
    }

    static private Model
    load(final File folder, Loop<Service> service) throws Exception {
        if (!folder.equals(dbDir) && !folder.getPath().startsWith(dbDirPath)) {
            throw new IOException();
        }

        // The caller has read/write access to a raw persistence folder,
        // so assume the caller is trusted infrastructure code.
        final JODB r = new JODB(folder, service);
        live.put(folder, new SoftReference<JODB>(r));
        return r;
    }

    // org.waterken.model.Model interface

    /**
     * Is a {@link #enter transaction} currently being processed?
     */
    private boolean busy = false;
    
    /**
     * Has the model been awoken?
     */
    private boolean awake = false;

    /**
     * Processes a transaction within this model.
     */
    public synchronized <R> R
    enter(final boolean extend, final Transaction<R> body) throws Exception {
        if (busy) { throw new AssertionError(); }
        busy = true;

        final Promise<R> r;
        try {
            if (!awake) {
                process(Model.extend, new Transaction<Void>() {
                    public Void
                    run(final Root local) throws Exception {
                        final Transaction<?> wake =
                            (Transaction)local.fetch(null, Root.wake);
                        if (null != wake) { wake.run(local); }
                        return null;
                    }
                });
                awake = true;
            }
            r = process(extend, body);
        } catch (final Exception e) {
            throw new ModelError(e);
        } finally {
            busy = false;
        }
        return r.cast();
    }

    /**
     * An object store entry.
     */
    static final class
    Bucket extends Struct {
        final boolean created;          // Is a newly created bucket?
        final Object value;             // contained object
        final ByteArray fingerprint;    // secure hash of serialization, or
                                        // <code>null</code> if not known

        Bucket(final boolean created,
               final Object value,
               final ByteArray fingerprint) {
            this.created = created;
            this.value = value;
            this.fingerprint = fingerprint;
        }
    }

    <R> Promise<R>
    process(final boolean extend, final Transaction<R> body) throws Exception {
        // Is the current transaction still active?
        final boolean[] active = { true };

        // Setup tables to keep track of what's been loaded from disk
        // and what needs to be written out to disk.
        final HashMap<String,Bucket> k2o =          // [ file key => bucket ]
            new HashMap<String,Bucket>(64);
        final IdentityHashMap<Object,Integer> o2d = // [ object => object id ]
            new IdentityHashMap<Object,Integer>(64);
        final HashSet<String> xxx =                 // [ dirty file key ]
            new HashSet<String>(64);
        class Loader implements Heap {

            public Object
            reference(final long address) throws NullPointerException {
                if (!active[0]) { throw new AssertionError(); }

                if (0 != type(address)) { throw new NullPointerException(); }
                return load(state(address)).value;
            }

            public long
            locate(final Object object) {
                if (!active[0]) { throw new AssertionError(); }

                return address(0, id(object));
            }

            private int
            id(final Object object) {
                Integer d = o2d.get(object);
                if (null == d) {
                    if (Slicer.inline(object)) {
                        // Reuse an equivalent persistent identity;
                        // otherwise, determine the persistent identity.
                        final ByteArray fingerprint;
                        try {
                            final MessageDigest hash = allocHash();
                            final Slicer out = new Slicer(object, this,
                                    new DigestOutputStream(sink, hash));
                            out.writeObject(object);
                            out.flush();
                            out.close();
                            fingerprint = ByteArray.array(hash.digest());
                            freeHash(hash);
                        } catch (final IOException e) {
                            throw new ModelError(e);
                        } catch (final NoSuchAlgorithmException e) {
                            throw new ModelError(e); // should never happen
                        }
                        final int hash = fingerprint.hashCode();
                        final int step = hash | 1;
                        for (int probe = hash; true;) {
                            final Bucket x = load(probe);
                            if (null == x) {
                                d = probe;
                                final String key = key(d);
                                k2o.put(key,
                                        new Bucket(true, object, fingerprint));
                                o2d.put(object, d);
                                xxx.add(key);
                                break;
                            }
                            if (fingerprint.equals(x.fingerprint)) {
                                d = probe;
                                break;
                            }
                            probe += step;
                            if (hash == probe) {
                                throw new ModelError(new IOException("full"));
                            }
                        }
                    } else {
                        // Assign a new persistent identity.
                        String key;
                        for (int i = 0; true;) {
                            d = prng.nextInt();
                            key = key(d);
                            if (!(k2o.containsKey(key) ||
                                  file(folder, key + ext).isFile())) {
                                break;
                            }
                            if (0 == ++i) {
                                throw new ModelError(new IOException("full"));
                            }
                        }
                        k2o.put(key, new Bucket(true, object, null));
                        o2d.put(object, d);
                        xxx.add(key);
                    }
                }
                return d;
            }

            /**
             * for detecting cyclic object graphs
             */
            private final Bucket pumpkin = new Bucket(false, null, null);
            private final ArrayList<String> stack = new ArrayList<String>(16);

            /**
             * Loads a stored object graph.
             * @param d file identifier
             * @return corresponding bucket, or <code>null</code> if none
             */
            private Bucket
            load(final int d) {
                final String key = key(d);
                Bucket r = k2o.get(key);
                if (null == r) {
                    final File f = file(folder, key + ext);
                    if (f.isFile()) {
                        
                        k2o.put(key, pumpkin);
                        stack.add(key);
                        r = read(f);
                        k2o.remove(key);
                        stack.remove(stack.size() - 1);

                        k2o.put(key, r);
                        o2d.put(r.value, d);
                        if (!frozen(r.value)) { xxx.add(key); }
                    }
                } else if (pumpkin == r) {
                    // Hit a cyclic reference.

                    // Determine the cycle depth.
                    int n = 0;
                    for (int i = stack.size(); i-- != 0;) {
                        ++n;
                        if (key.equals(stack.get(i))) { break; }
                    }

                    // Determine the type of object in each file.
                    final Class[] type = new Class[n];
                    for (int i = stack.size(); n-- != 0;) {
                        final String at = stack.get(--i);
                        type[n] = Object.class;
                        InputStream fin = null;
                        try {
                            fin = Filesystem.read(file(folder, at + ext));
                            final SubstitutionStream in =
                                new SubstitutionStream(true, code, fin) {
                                protected Object
                                resolveObject(Object x) throws IOException {
                                    return x instanceof Wrapper ? null : x;
                                }
                            };
                            fin = in;
                            final Object x = in.readObject();
                            type[n] = null != x ? x.getClass() : Void.class;
                        } catch (final Exception e) {
                            // Can't figure out the type.
                        }
                        if (null != fin) {
                            try { fin.close(); } catch (IOException e) {}
                        }
                    }
                    throw new CyclicGraph(PowerlessArray.array(type));
                }
                return r;
            }

            /**
             * Reads a stored object graph.
             * @param f root file
             * @return corresponding bucket
             */
            Bucket
            read(final File f) {
                InputStream fin = null;
                try {
                    final MessageDigest hash = allocHash();
                    fin = Filesystem.read(f);
                    final SubstitutionStream in = new SubstitutionStream(true,
                            code, new DigestInputStream(fin, hash)) {
                        protected Object
                        resolveObject(final Object x) throws IOException {
                            return x instanceof Wrapper
                                ? ((Wrapper)x).peel(Loader.this)
                                : x;
                        }
                    };
                    fin = in;
                    final Object value = in.readObject();
                    fin.close();
                    final ByteArray fingerprint= ByteArray.array(hash.digest());
                    freeHash(hash);
                    return new Bucket(false, value, fingerprint);
                } catch (final Exception e) {
                    if (null != fin) {
                        try { fin.close(); } catch (final Exception e2) {}
                    }
                    if (e instanceof CyclicGraph) { throw (CyclicGraph)e; }
                    throw new ModelError(e);
                }
            }
        };
        final Loader loader = new Loader();
        final Root root = new Root() {

            public Object
            fetch(final Object otherwise, final String name) {
                if (!active[0]) { throw new AssertionError(); }

                final String key = name.toLowerCase();
                Bucket x = k2o.get(key);
                if (null == x) {
                    final File f;
                    try {
                        f = file(folder, key + ext);
                    } catch (final InvalidFilenameException e) {
                        return otherwise;
                    }
                    if (!f.isFile()) { return otherwise; }
                    x = loader.read(f);
                    if (x.value instanceof SymbolicLink) {
                        k2o.put(key, x);
                    }
                }
                if (x.value instanceof SymbolicLink) {
                    return ((SymbolicLink)x.value).target;
                }
                return otherwise;
            }

            public void
            store(final String name, final Object value) {
                if (!active[0]) { throw new AssertionError(); }
                if (extend) { throw new ProhibitedModification(); }

                final String key = name.toLowerCase();
                Filesystem.checkName(key + ext);
                k2o.put(key, new Bucket(true, new SymbolicLink(value), null));
                xxx.add(key);
            }
        };
        final boolean[] scheduled = { false };
        final Loop<Task> enqueue = new Loop<Task>() {
            @SuppressWarnings("unchecked") public void
            run(final Task task) {
                if (!active[0]) { throw new AssertionError(); }

                List<Task> q = (List)root.fetch(null, tasks);
                if (null == q) {
                    q = List.list();
                    root.store(tasks, q);
                }
                q.append(task);
                scheduled[0] = true;
            }
        };
        final List<Effect> effects = List.list();
        final Loop<Effect> effect = new Loop<Effect>() {
            public void
            run(final Effect task) { effects.append(task); }
        };
        final Runnable destruct = new Runnable() {
            public void
            run() {
                root.store(dead, true);
                effect.run(new Effect() {
                    public void
                    run() {
                        service.run(new Service() {
                            public void
                            run() throws Exception {
                                final File tmp = file(
                                    folder.getParentFile(),
                                    "." + folder.getName() + ".tmp");
                                while (!folder.renameTo(tmp) &&
                                       folder.isDirectory()) {
                                    Thread.sleep(60 * 1000);
                                }
                                delete(tmp);
                            }
                        });
                    }
                });
            }
        };
        final boolean[] modified = { false };
        final File pending = file(folder, ".pending");
        final Creator create = new Creator() {
            public <T> T
            run(final String label,
                    final Transaction<T> initialize, final String project) {
                if (!active[0]) { throw new AssertionError(); }
                if (extend) { throw new ProhibitedModification(); }
                if (null == project || "".equals(project)) {
                    throw new NullPointerException();
                }
                if(label.startsWith(".")){throw new InvalidFilenameException();}

                if (!modified[0]) {
                    if (!pending.mkdir()) { throw new Error(); }
                    modified[0] = true;
                }
                final String key = label.toLowerCase();
                if (file(folder, "." + key + ".was").isFile()) {
                    throw new NoLabelReuse();
                }
                try {
                    if (!file(pending, "." + key + ".was").createNewFile()) {
                        throw new NoLabelReuse();
                    }
                } catch (final IOException e) {
                    throw new ModelError(e);
                }
                final File sub = file(pending, key);
                try {
                    if (!sub.mkdir()) { throw new IOException(); }
                    OutputStream out =
                        Filesystem.writeNew(file(sub, Root.project + ext));
                    try {
                        final ObjectOutputStream oout =
                            new ObjectOutputStream(out);
                        out = oout;
                        oout.writeObject(new SymbolicLink(project));
                        oout.flush();
                    } catch (final Exception e) {
                        try { out.close(); } catch (final Exception e2) {}
                        throw e;
                    }
                    out.close();
                    return new JODB(sub, null).enter(change, initialize);
                } catch (final Exception e) {
                    throw new ModelError(e);
                }
            }
        };

        // Setup the pseudo-persistent objects.
        final String[] name = {
            Root.nothing, Root.code, Root.prng, Root.heap,
            ".root", Root.enqueue, Root.effect, Root.destruct,
            Root.model, Root.create
        };
        final Object[] value = {
            null, code, prng, loader,
            root, enqueue, effect, destruct,
            this, create
        };
        for (int d = 0; value.length != d; ++d) {
            k2o.put(name[d],new Bucket(false,new SymbolicLink(value[d]),null));
            k2o.put(key(d), new Bucket(false, value[d], null));
            o2d.put(value[d], d);
        }

        // Save some IDs for future expansion of the default scope.
        for (int d = value.length; 16 != d; ++d) {
            k2o.put(key(d), new Bucket(false, null, null));
        }

        // Recover from a previous run.
        final File committed = file(folder, ".committed");
        if (pending.isDirectory()) {
            if (committed.isDirectory()) { delete(committed); }
            delete(pending);
        } else if (committed.isDirectory()) {
            move(committed, folder);
        }
        
        // Check that the model has not been destructed.
        if (!folder.isDirectory() ||
                Boolean.TRUE.equals(root.fetch(false, dead))) {
            return new Rejected<R>(new FileNotFoundException());
        }

        // Execute the transaction body.
        Promise<R> r;
        try {
            r = Fulfilled.detach(body.run(root));
        } catch (final Exception e) {
            r = new Rejected<R>(e);
        }

        // Persist the modifications.
        final MessageDigest hash = allocHash();
        while (!xxx.isEmpty()) {
            final Iterator<String> i = xxx.iterator();
            final String key = i.next();
            final Bucket x = k2o.get(key);
            i.remove();

            OutputStream fout;
            if (extend && !x.created) {
                fout = new DigestOutputStream(sink, hash) {
                    public void
                    close() throws IOException {
                        super.close();
                        final ByteArray fingerprint =
                            ByteArray.array(hash.digest());
                        if (!fingerprint.equals(x.fingerprint)) {
                            throw new ProhibitedModification();
                        }
                    }
                };
            } else {
                if (!modified[0]) {
                    if (!pending.mkdir()) { throw new IOException(); }
                    modified[0] = true;
                }
                fout = Filesystem.writeNew(file(pending, key + ext));
            }
            try {
                final Slicer out = new Slicer(x.value, loader, fout);
                fout = out;
                out.writeObject(x.value);
                out.flush();
            } catch (final Exception e) {
                try { fout.close(); } catch (final Exception e2) {}
                throw e;
            }
            fout.close();
        }
        freeHash(hash);

        if (modified[0]) {
            // Commit modifications.
            if (!pending.renameTo(committed)) { throw new IOException(); }
            move(committed, folder);
        } else {
            // No modifications were made, so just make sure all the reads
            // had valid state available.
            if (!folder.isDirectory()) { throw new FileNotFoundException(); }
        }

        // Run all the pending effects.
        while (!effects.isEmpty()) { effects.pop().run(); }

        // Start up a runner if necessary.
        if (null == runner && scheduled[0]) {
            final Run x = new Run();
            service.run(x);
            runner = x;
        }
        
        return r;
    }

    // In testing, allocation of hash objects doubled serialization time,
    // so I'm keeping a pool of them. Sucky code is like cancer.
    private final ArrayList<MessageDigest> sha256 =
        new ArrayList<MessageDigest>();

    private MessageDigest
    allocHash() throws NoSuchAlgorithmException {
        return sha256.isEmpty()
            ? MessageDigest.getInstance("SHA-256")
        : sha256.remove(sha256.size() - 1);
    }

    private void
    freeHash(final MessageDigest h) { sha256.add(h); }

    /**
     * an output sink
     */
    static private final OutputStream sink = new OutputStream() {

        public void
        write(byte[] b, int off, int len) {}

        public void
        write(int b) {}
    };

    /**
     * corresponding {@link Destructor} flag
     */
    static private final String dead = ".dead";

    /**
     * {@link Root} name for the pending task {@link List}
     */
    static private final String tasks = ".tasks";

    /**
     * pending {@link Run} task, if any
     */
    private transient Run runner;

    /**
     * Process a single event from the stored event queue.
     */
    class Run implements Service {
        public void
        run() throws Exception {
            enter(change, new Transaction<Void>() {
                @SuppressWarnings("unchecked") public Void
                run(final Root local) throws Exception {
                    runner = null;
                    final List<Task> q = (List)local.fetch(null, tasks);
                    try {
                        q.pop().run();
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
                    if (!q.isEmpty()) {
                        ((Loop<Effect>)local.fetch(null, Root.effect)).
                            run(new Effect() {
                                public void
                                run() throws Exception {
                                    service.run(Run.this);
                                    runner = Run.this;
                                }
                            });
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Move each file in the source directory to the destination directory.
     * @param from  source directory
     * @param to    destination directory
     * @throws IOException  file manipulation failed
     */
    static private void
    move(final File from, final File to) throws IOException {
        for (final File src : from.listFiles()) {
            final File dst = new File(to, src.getName());
            dst.delete();
            if (!src.renameTo(dst)) { throw new IOException(); }
        }
        if (!from.delete()) { throw new IOException(); }
    }

    /**
     * Recursively delete a directory.
     * @param dir   directory to recursively delete
     * @throws IOException  delete operation failed
     */
    static private void
    delete(final File dir) throws IOException {
        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                delete(f);
            } else {
                if (!f.delete()) { throw new IOException(); }
            }
        }
        if (!dir.delete()) { throw new IOException(); }
    }

    /**
     * bit mask for turning an int into a long
     */
    static private final long intBits = (1L << Integer.SIZE) - 1;

    /**
     * Make a fat pointer.
     * @param t pointer type
     * @param d object state
     * @return fat pointer
     */
    static private long
    address(final int t, final int d) {
        return ((t & intBits) << Integer.SIZE) | (d & intBits);
    }

    /**
     * Get the fat pointer type.
     * @param fat pointer
     * @return pointer type
     */
    static private int
    type(final long a) { return (int)(a >>> Integer.SIZE); }

    /**
     * Get the fat pointer state.
     * @param fat pointer
     * @return pointer state
     */
    static private int
    state(final long a) { return (int)a; }

    /**
     * file extension for a serialized Java object tree
     */
    static         final String ext = ".jos";

    /**
     * name prefix for generated file keys
     */
    static         final String prefix = ".@";

    /**
     * Turn an object identifier into a file key.
     * @param d object identifier
     * @return file key
     */
    static private String
    key(final int d) {
        final int n = Integer.SIZE;
        final StringBuilder r = new StringBuilder(prefix.length() + n / 4);
        r.append(prefix);
        for (int i = n; i != 0;) {
            i -= 4;
            r.append(hex((d >>> i) & 0x0F));
        }
        return r.toString();
    }

    static private char
    hex(final int i) { return (char)(i < 10 ? '0' + i : 'A' + (i - 10)); }

    /**
     * Is the object one-level deep immutable?
     * @param x candidate object
     * @return true if the object is one-level deep immutable,
     *         false if it might not be
     */
    static private boolean
    frozen(final Object x) {
        return null == x ||
               JoeE.instanceOf(x, org.joe_e.Selfless.class) ||
               JoeE.instanceOf(x, org.joe_e.Immutable.class);
    }
}
