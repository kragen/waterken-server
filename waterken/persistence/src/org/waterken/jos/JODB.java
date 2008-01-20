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
import java.lang.ref.ReferenceQueue;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

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
import org.waterken.cache.Cache;
import org.waterken.model.Base32;
import org.waterken.model.Creator;
import org.waterken.model.CyclicGraph;
import org.waterken.model.Effect;
import org.waterken.model.Model;
import org.waterken.model.ProhibitedCreation;
import org.waterken.model.ProhibitedModification;
import org.waterken.model.Root;
import org.waterken.model.Service;
import org.waterken.model.Transaction;
import org.waterken.model.UnknownClass;
import org.waterken.thread.Concurrent;
import org.web_send.graph.Collision;
import org.web_send.graph.Unavailable;

/**
 * An object graph stored as a folder of Java Object Serialization files.
 */
public final class
JODB extends Model {

    /**
     * canonical persistence folder
     */
    private final File folder;

    private
    JODB(final File folder, final Loop<Service> service) {
        super(service);
        this.folder = folder;
    }

    /**
     * file path to the folder containing all persistence folders
     */
    static public  final String homePathProperty = "waterken.home";
    static private final File home;
    static public  final String dbPathProperty = "waterken.db";
    static public  final String dbPathDefault = "db";
    static private final File dbDir;
    static private final String dbDirPath;
    static {
        try {
            home = new File(
                System.getProperty(homePathProperty, "")).getCanonicalFile();
            final String dbPathConfig = System.getProperty(dbPathProperty);
            dbDir = (null != dbPathConfig
                ? new File(dbPathConfig)
            : new File(home, dbPathDefault)).getCanonicalFile();
            dbDirPath = dbDir.getPath() + File.separator;
        } catch (final IOException e) {
            throw new Error(e);
        }
    }
    static private final Cache<File,JODB> live =
        new Cache<File,JODB>(new ReferenceQueue<JODB>());

    /**
     * Opens an existing, but not yet open, model.
     * @param id        persistence folder
     * @param service   {@link #service}
     * @throws FileNotFoundException    <code>id</code> not a persistence folder
     */
    static public Model
    open(final File id, final Loop<Service> service) throws Exception {
        final File folder = id.getCanonicalFile();
        if (!folder.isDirectory()) { throw new FileNotFoundException(); }
        synchronized (live) {
            if (null != live.fetch(null, folder)) { throw new Exception(); }
            return load(folder, service);
        }
    }

    static private final ThreadGroup threads = new ThreadGroup("db");

    /**
     * Connects to an existing model.
     * <p>
     * If the model is not yet {@link #open open}, it will be opened with a
     * {@link Concurrent#loop concurrent} {@link #service} loop.
     * </p>
     * @param id    persistence folder
     * @throws FileNotFoundException    <code>id</code> not a persistence folder
     */
    static public Model
    connect(final File id) throws Exception {
        final File folder = id.getCanonicalFile();
        if (!folder.isDirectory()) { throw new FileNotFoundException(); }
        synchronized (live) {
            final JODB r = live.fetch(null, folder);
            if (null != r) { return r; }
            final Loop<Service> service = Concurrent.loop(threads,
                folder.getPath().substring(dbDir.getPath().length()));
            return load(folder, service);
        }
    }

    static private Model
    load(final File folder,
         final Loop<Service> service) throws FileNotFoundException {
        if (!folder.equals(dbDir) && !folder.getPath().startsWith(dbDirPath)) {
            throw new FileNotFoundException();
        }

        // The caller has read/write access to a raw persistence folder,
        // so assume the caller is trusted infrastructure code.
        final JODB r = new JODB(folder, service);
        live.put(folder, r);
        return r;
    }

    // org.waterken.model.Model interface

    /**
     * Is a {@link #enter transaction} currently being processed?
     */
    private boolean busy = false;

    /**
     * Has the event loop been restarted?
     */
    private boolean awake = false;

    /**
     * Processes a transaction within this model.
     */
    public synchronized <R> Promise<R>
    enter(final boolean extend, final Transaction<R> body) throws Exception {
        if (busy) { throw new Exception(); }
        busy = true;
        try {
            if (!awake) {
                awake = process(Model.extend, new Transaction<Boolean>() {
                    public Boolean
                    run(final Root local) throws Exception {
                        // start up a runner if necessary
                        if (null == runner && null != service) {
                            final List q = (List)local.fetch(null, tasks);
                            if (null != q && !q.isEmpty()) {
                                final Run x = new Run();
                                service.run(x);
                                runner = x;
                            }
                        }

                        // start up any other configured services
                        final Transaction<?> wake =
                            (Transaction)local.fetch(null, Root.wake);
                        if (null != wake) { wake.run(local); }

                        return true;
                    }
                }).cast();
            }
            return process(extend, body);
        } catch (final Error e) {
            // allow the caller to recover from an aborted transaction
            if (e instanceof OutOfMemoryError) { System.gc(); }
            final Throwable cause = e.getCause();
            if (cause instanceof Exception) { throw (Exception)cause; }
            throw new Exception(e);
        } finally {
            busy = false;
        }
    }

    /**
     * An object store entry.
     */
    static final class
    Bucket extends Struct {
        final boolean created;      // Is a newly created bucket?
        final Object value;         // contained object
        final ByteArray version;    // secure hash of serialization, or
                                    // <code>null</code> if not known

        Bucket(final boolean created,
               final Object value,
               final ByteArray version) {
            this.created = created;
            this.value = value;
            this.version = version;
        }
    }

    /**
     * {@link Root#prng}
     */
    private SecureRandom prng;

    /**
     * {@link Root#code}
     */
    private ClassLoader code;

    <R> Promise<R>
    process(final boolean extend, final Transaction<R> body) throws Exception {
        // finish object initialization, which was delayed to avoid doing
        // anything intensive while holding the global "live" lock
        if (null == prng) { prng = new SecureRandom(); }
        if (null == code) { code = application(folder); }

        // Is the current transaction still active?
        final boolean[] active = { true };

        // setup tables to keep track of what's been loaded from disk
        // and what needs to be written out to disk
        final HashMap<String,Bucket> k2b =          // [ file key => bucket ]
            new HashMap<String,Bucket>(64);
        final IdentityHashMap<Object,String> o2k =  // [ object => file key ]
            new IdentityHashMap<Object,String>(64);
        final HashSet<String> xxx =                 // [ dirty file key ]
            new HashSet<String>(64);
        final Root root = new Root() {

            public String
            getModelName() { return folder.getName(); }

            public String
            pipeline(final String m) {
                final byte[] key = Base32.decode(m);
                if (128/Byte.SIZE != key.length) {throw new RuntimeException();}
                try {
                    final byte[] plaintext = new byte[128 / Byte.SIZE];
                    final byte[] cyphertext = new byte[128 / Byte.SIZE];
                    final Cipher aes = Cipher.getInstance("AES/ECB/NoPadding");
                    aes.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key,"AES"));
                    aes.doFinal(plaintext, 0, plaintext.length, cyphertext);
                    return Base32.encode(cyphertext);
                } catch (final Exception e) { throw new Error(e); }
            }

            /**
             * Creates an object store entry.
             * <p>
             * Each entry is stored as a file in the persistence folder. The
             * corresponding filename is of the form XYZ.jos, where the XYZ
             * component may be chosen by the client and the ".jos" extension is
             * automatically appended by the implementation. All filenames are
             * composed of only lower case letters. For example,
             * ".stuff.jos.jos" is the filename corresponding to the user chosen
             * name ".Stuff.JOS". The file extension chosen by the user carries
             * no significance in this implementation, and neither does a
             * filename starting with a "." character. All filenames are vetted
             * by the Joe-E filesystem API.
             * </p>
             * <p>
             * The filename generated by {@link #export} is considered the
             * canonical name for an object. Names assigned to an object via
             * {@link #link} are valid for lookup, but will not be returned by
             * {@link #export}. This feature is implemented by wrapping a
             * {@linkplain #link linked} object in a {@link SymbolicLink}, and
             * unwrapping it on {@link #fetch}.
             * </p>
             */
            public void
            link(final String name, final Object value) throws Collision {
                if (!active[0]) { throw new AssertionError(); }
                if (extend) { throw new ProhibitedModification(Root.class); }

                final String key = name.toLowerCase();
                try {
                    if (null != load(key)) { throw new Collision(); }
                } catch (final Exception e) { throw new Collision(); }
                Filesystem.checkName(key + ext);
                k2b.put(key, new Bucket(true, new SymbolicLink(value), null));
                xxx.add(key);
            }

            public Object
            fetch(final Object otherwise, final String name) {
                if (!active[0]) { throw new AssertionError(); }

                final Bucket b = load(name.toLowerCase());
                return null == b
                    ? otherwise
                : b.value instanceof SymbolicLink
                    ? ((SymbolicLink)b.value).target
                : b.value;
            }

            /**
             * for detecting cyclic object graphs
             */
            private final Bucket pumpkin = new Bucket(false, null, null);
            private final ArrayList<String> stack = new ArrayList<String>(16);

            private Bucket
            load(final String key) {
                Bucket bucket = k2b.get(key);
                if (null == bucket) {
                    final File file;
                    try {
                        file = file(folder, key + ext);
                    } catch (final InvalidFilenameException e) { return null; }
                    if (!file.isFile()) { return null; }

                    k2b.put(key, pumpkin);
                    stack.add(key);
                    try {
                        bucket = read(file);
                    } finally {
                        k2b.remove(key);
                        stack.remove(stack.size() - 1);
                    }

                    k2b.put(key, bucket);
                    o2k.put(bucket.value, key);
                    if (!frozen(bucket.value)) { xxx.add(key); }
                } else if (pumpkin == bucket) {
                    // hit a cyclic reference

                    // determine the cycle depth
                    int n = 0;
                    for (int i = stack.size(); i-- != 0;) {
                        ++n;
                        if (key.equals(stack.get(i))) { break; }
                    }

                    // determine the type of object in each file
                    final Class[] type = new Class[n];
                    for (int i = stack.size(); n-- != 0;) {
                        final String at = stack.get(--i);
                        type[n] = Object.class;
                        InputStream fin = null;
                        try {
                            final int pos = n;
                            fin = Filesystem.read(file(folder, at + ext));
                            final SubstitutionStream in= new SubstitutionStream(
                                    true, JODB.this.code, fin) {
                                protected Object
                                resolveObject(Object x) {
                                    if (x instanceof Wrapper) {
                                        type[pos] = x.getClass();
                                        x = null;
                                    }
                                    return x;
                                }
                            };
                            fin = in;
                            final Object x = in.readObject();
                            if (null != x) { type[n] = x.getClass(); }
                        } catch (final Exception e) { /* unknown type */ }
                        if(null!=fin){ try{fin.close();}catch(IOException e){} }
                    }
                    throw new CyclicGraph(PowerlessArray.array(type));
                }
                return bucket;
            }
            
            private boolean externalStateAccessed = false;

            /**
             * Reads a stored object graph.
             * @param file  containing file
             * @return corresponding bucket
             */
            private Bucket
            read(final File file) {
                InputStream fin = null;
                try {
                    fin = Filesystem.read(file);
                    final Mac mac = allocMac(this);
                    fin = new MacInputStream(fin, mac);
                    final Root loader = this;
                    final SubstitutionStream in =
                            new SubstitutionStream(true, JODB.this.code, fin) {
                        protected Object
                        resolveObject(Object x) throws IOException {
                            if (x instanceof File) {
                                externalStateAccessed = true;
                            } else if (x instanceof Wrapper) {
                                x = ((Wrapper)x).peel(loader);
                            }
                            return x;
                        }
                    };
                    fin = in;
                    final Object value = in.readObject();
                    fin.close();
                    final byte[] version = mac.doFinal();
                    freeMac(mac);
                    return new Bucket(false, value, ByteArray.array(version));
                } catch (final Exception e) {
                    if (null != fin) {try {fin.close();} catch (Exception x){}}
                    if (e instanceof IOException) { throw new Error(e); }
                    if(e instanceof RuntimeException){throw(RuntimeException)e;}
                    if (e instanceof ClassNotFoundException) {
                        throw new UnknownClass(e.getMessage());
                    }
                    throw new RuntimeException(e);
                }
            }
            
            public String
            getTransactionTag() {
                if (!active[0]) { throw new AssertionError(); }
                
                if (externalStateAccessed) { return null; }

                final Mac mac;
                try {
                    mac = allocMac(this);
                } catch (final Exception e) { throw new Error(e); }
                for (final Bucket b : k2b.values()) {
                    if (!b.created && null != b.version) {
                        mac.update(b.version.toByteArray());
                    }
                }
                if (JODB.this.code instanceof Project) {
                    // include code timestamp in the ETag
                    long buffer = ((Project)JODB.this.code).timestamp;
                    for (int i = Long.SIZE; 0 != i; i -= Byte.SIZE) {
                        mac.update((byte)buffer);
                        buffer >>>= Byte.SIZE;
                    }
                }
                final byte[] id = mac.doFinal();
                freeMac(mac);
                return '\"' + Base32.encode(id).substring(0, 2*keyChars) + '\"';
            }

            public String
            export(final Object value) {
                if (!active[0]) { throw new AssertionError(); }

                String key = o2k.get(value);
                if (null == key) {
                    if (Slicer.inline(value)) {
                        // reuse an equivalent persistent identity;
                        // otherwise, determine the persistent identity
                        final byte[] id;
                        try {
                            final Mac mac = allocMac(this);
                            final Slicer out = new Slicer(value, this,
                                    new MacOutputStream(sink, mac));
                            out.writeObject(value);
                            out.flush();
                            out.close();
                            id = mac.doFinal();
                            freeMac(mac);
                        } catch (final Exception e) {
                            throw new Error(e);
                        }
                        final ByteArray version = ByteArray.array(id);
                        final byte[] d = new byte[keyBytes];
                        System.arraycopy(id, 0, d, 0, d.length);
                        while (true) {
                            key = key(d);
                            try {
                                final Bucket b = load(key);
                                if (null == b) {
                                    k2b.put(key,new Bucket(true,value,version));
                                    o2k.put(value, key);
                                    xxx.add(key);
                                    break;
                                }
                                if (version.equals(b.version)) { break; }
                            } catch (final Exception e) {/*skip broken bucket*/}
                            for (int i = 0; i != d.length; ++i) {
                                if (0 != ++d[i]) { break; }
                            }
                        }
                    } else {
                        // to support caching of query responses, forbid
                        // creation of selfish state in an extend transaction
                        if (extend) {
                            throw new ProhibitedCreation(value.getClass());
                        }

                        // assign a new persistent identity
                        do {
                            final byte[] d = new byte[keyBytes];
                            JODB.this.prng.nextBytes(d);
                            key = key(d);
                        } while (k2b.containsKey(key) ||
                                 file(folder, key + ext).isFile());
                        k2b.put(key, new Bucket(true, value, null));
                        o2k.put(value, key);
                        xxx.add(key);
                    }
                }
                return key;
            }
        };
        final boolean[] scheduled = { false };
        final Loop<Task> enqueue = new Loop<Task>() {
            public void
            run(final Task task) {
                if (!active[0]) { throw new AssertionError(); }

                try {
                    final List<Task> q = (List)root.fetch(null, tasks);
                    q.append(task);
                    scheduled[0] = true;
                } catch (final Exception e) {}
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
                if (!active[0]) { throw new AssertionError(); }

                root.link(dead, true);
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
        final Creator creator = new Creator() {

            public ClassLoader
            load(final String project) throws Exception { return jar(project); }

            public <T> T
            create(final Transaction<T> initialize,
                   final String project, final String name) throws Exception {
                if (!active[0]) { throw new AssertionError(); }
                if (extend) { throw new ProhibitedModification(Creator.class); }

                if (!modified[0]) {
                    if (!pending.mkdir()) {throw new Error(new IOException());}
                    modified[0] = true;
                }

                final String key;
                if (null != name) {
                    key = claim(name);
                } else {
                    String x = null;
                    while (true) {
                        try {
                            final byte[] d = new byte[4];
                            prng.nextBytes(d);
                            x = claim(Base32.encode(d).substring(0, 6));
                            break;
                        } catch (final Collision e) {}
                    }
                    key = x;
                }
                final File sub = file(pending, key);
                if (!sub.mkdir()) { throw new Error(new IOException()); }
                if (null != project) { setConfig(sub, Root.project, project); }
                final byte[] bits = new byte[128 / Byte.SIZE];
                prng.nextBytes(bits);
                setConfig(sub, secret, ByteArray.array(bits));
                final Promise<T> r;
                try {
                    r = new JODB(sub, null).enter(change, new Transaction<T>() {
                        public T
                        run(final Root local) throws Exception {
                            final List<Task> q = List.list();
                            local.link(tasks, q);
                            return initialize.run(local);
                        }
                    });
                } catch (final Exception e) {
                    throw new Error(e);
                }
                return r.cast();
            }

            /**
             * Claims a sub-model name.
             * <p>
             * The persistence folder may also contain sub-folders named by the
             * client; however, the implementation forbids use of names that
             * start with a "." character. Folder names starting with a "." are
             * reserved for use by the implementation. For example, folders
             * containing pending modifications, or folders to be deleted, have
             * names starting with a ".".
             * </p>
             * @param name  model name to claim
             * @return canonicalized model name
             * @throws Collision    <code>name</code> is not available
             */
            private String
            claim(final String name) throws Collision {
                final String key = name.toLowerCase();
                if ("".equals(key)) { throw new Collision(); }
                if (key.startsWith(".")) { throw new Unavailable(); }

                final String was = "." + key + ".was";
                try {
                    if (file(folder, was).isFile()) { throw new Collision(); }
                } catch (InvalidFilenameException e) {throw new Unavailable();}
                try {
                    if (!file(pending, was).createNewFile()) {
                        throw new Collision();
                    }
                } catch (final IOException e) { throw new Error(e); }
                return key;
            }
        };

        // setup the pseudo-persistent objects
        k2b.put(Root.nothing, new Bucket(false, null, null));
        k2b.put(Root.code, new Bucket(false, code, null));
        k2b.put(Root.prng, new Bucket(false, prng, null));
        k2b.put(".root", new Bucket(false, root, null));
        k2b.put(Root.enqueue, new Bucket(false, enqueue, null));
        k2b.put(Root.effect, new Bucket(false, effect, null));
        k2b.put(Root.destruct, new Bucket(false, destruct, null));
        k2b.put(Root.model, new Bucket(false, this, null));
        k2b.put(Root.creator, new Bucket(false, creator, null));
        for (final Map.Entry<String,Bucket> x : k2b.entrySet()) {
            o2k.put(x.getValue().value, x.getKey());
        }

        // recover from a previous run
        final File committed = file(folder, ".committed");
        if (pending.isDirectory()) {
            if (committed.isDirectory()) { delete(committed); }
            delete(pending);
        } else if (committed.isDirectory()) {
            move(committed, folder);
        }

        // check that the model has not been destructed
        if (!folder.isDirectory() ||
                Boolean.TRUE.equals(root.fetch(false, dead))) {
            return new Rejected<R>(new FileNotFoundException());
        }

        // execute the transaction body
        Promise<R> r;
        try {
            try {
                r = Fulfilled.detach(body.run(root));
            } catch (final Exception e) {
                r = new Rejected<R>(e);
            }

            // persist the modifications
            while (!xxx.isEmpty()) {
                final Iterator<String> i = xxx.iterator();
                final String key = i.next();
                final Bucket b = k2b.get(key);
                i.remove();

                OutputStream fout;
                if (extend && !b.created) {
                    fout = new MacOutputStream(sink, allocMac(root)) {
                        public void
                        close() throws IOException {
                            super.close();
                            final ByteArray v = ByteArray.array(mac.doFinal());
                            freeMac(mac);
                            if (!v.equals(b.version)) {
                                throw new ProhibitedModification(null != b.value
                                    ? b.value.getClass() : Void.class);
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
                    final Slicer out = new Slicer(b.value, root, fout);
                    fout = out;
                    out.writeObject(b.value);
                    out.flush();
                } catch (final Exception e) {
                    try { fout.close(); } catch (final Exception e2) {}
                    throw e;
                }
                fout.close();
            }
        } finally {
            active[0] = false;
        }

        if (modified[0]) {
            // commit modifications
            if (!pending.renameTo(committed)) { throw new IOException(); }
            move(committed, folder);
        } else {
            // no modifications were made, so just make sure all the reads
            // had valid state available
            if (!folder.isDirectory()) { throw new IOException(); }
        }

        // run all the pending effects
        while (!effects.isEmpty()) { effects.pop().run(); }

        // start up a runner if necessary
        if (null == runner && scheduled[0] && null != service) {
            final Run x = new Run();
            service.run(x);
            runner = x;
        }

        return r;
    }

    /**
     * Gets a configuration value for a persistence folder.
     * @param folder    canonical persistence folder
     * @param key       configuration key
     */
    static private Object
    getConfig(final File folder, final String key) throws Exception {
        InputStream in = null;
        try {
            final File file = file(folder, key + ext);
            if (!file.isFile()) { return null; }
            in = Filesystem.read(file);
            final ObjectInputStream oin = new ObjectInputStream(in);
            in = oin;
            final Object r = oin.readObject();
            in.close();
            return r;
        } catch (final Exception e) {
            if (null != in) { try { in.close(); } catch (final Exception x) {} }
            if (e instanceof IOException) { throw new Error(e); }
            throw e;
        }
    }

    /**
     * Sets a configuration value for a persistence folder.
     * @param folder    canonical persistence folder
     * @param key       configuration key
     * @param value     configured value
     */
    static private void
    setConfig(final File folder,
              final String key, final Object value) throws Exception {
        OutputStream out = null;
        try {
            out = Filesystem.writeNew(file(folder, key + ext));
            final ObjectOutputStream oout = new ObjectOutputStream(out);
            out = oout;
            oout.writeObject(value);
            oout.flush();
            out.close();
        } catch (final Exception e) {
            if (null!=out) { try { out.close(); } catch (final Exception x) {} }
            if (e instanceof IOException) { throw new Error(e); }
            throw e;
        }
    }

    static private final Cache<String,ClassLoader> jars =
        new Cache<String,ClassLoader>(new ReferenceQueue<ClassLoader>());

    /**
     * Gets the named classloader.
     * @param project   project name
     */
    static private ClassLoader
    jar(final String project) throws Exception {
        if (null == project || "".equals(project)) {
            return JODB.class.getClassLoader();
        }
        Filesystem.checkName(project);

        synchronized (jars) {
            ClassLoader r = jars.fetch(null, project);
            if (null == r) {
                final String codePathConfig=System.getProperty("waterken.code");
                final File bins =
                    null != codePathConfig ? new File(codePathConfig) : home;
                final String jar = System.getProperty("waterken.bin",
                    File.separator + "bin" + File.separator);
                r = new Project(new File(bins, project + jar));
                jars.put(project, r);
            }
            return r;
        }
    }

    /**
     * Gets the corresponding classloader for a persistence folder.
     * @param folder    canonical persistence folder
     */
    static ClassLoader
    application(final File folder) throws Exception {
        return jar((String)getConfig(folder, Root.project));
    }

    /**
     * In testing, allocation of hash objects doubled serialization time, so I'm
     * keeping a pool of them. Sucky code is like cancer.
     */
    private final ArrayList<Mac> macs = new ArrayList<Mac>();

    /**
     * {@link Root} name for the master secret
     */
    static private final String secret = ".secret";

    /**
     * MAC key generation secret
     */
    private SecretKeySpec master;

    private Mac
    allocMac(final Root local) throws Exception {
        if (!macs.isEmpty()) { return macs.remove(macs.size() - 1); }
        if (null == master) {
            final ByteArray bits = (ByteArray)getConfig(folder, secret);
            master = new SecretKeySpec(bits.toByteArray(), "HmacSHA256");
        }
        final Mac r = Mac.getInstance("HmacSHA256");
        r.init(master);
        return r;
    }

    private void
    freeMac(final Mac h) { macs.add(h); }

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
                public Void
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
     * Move each file in the source folder to the destination folder.
     * @param from  source folder
     * @param to    destination folder
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
     * Recursively delete a folder.
     * @param dir   folder to recursively delete
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
     * file extension for a serialized Java object tree
     */
    static protected final String ext = ".jos";

    static private final int keyChars = 14;
    static private final int keyBytes = keyChars * 5 / 8 + 1;

    /**
     * Turn a bit string into a file key.
     * @param d object identifier
     * @return file key
     */
    static private String
    key(final byte[] d) { return Base32.encode(d).substring(0, keyChars); }

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
