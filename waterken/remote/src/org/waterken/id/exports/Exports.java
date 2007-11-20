// Copyright 2006-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.id.exports;

import java.io.Serializable;
import java.security.SecureRandom;

import org.joe_e.Struct;
import org.joe_e.Token;
import org.joe_e.array.ByteArray;
import org.joe_e.charset.URLEncoding;
import org.ref_send.Brand;
import org.ref_send.Record;
import org.ref_send.promise.Fulfilled;
import org.ref_send.promise.Volatile;
import org.ref_send.promise.eventual.Eventual;
import org.waterken.id.Exporter;
import org.waterken.id.Importer;
import org.waterken.model.Heap;
import org.waterken.model.Root;
import org.waterken.uri.Base32;
import org.waterken.uri.Path;
import org.waterken.uri.Query;
import org.waterken.uri.URI;
import org.web_send.graph.Collision;
import org.web_send.graph.Publisher;

/**
 * A case-insensitive name to value mapping.
 */
public final class
Exports implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs a reference exporter.
     * @param local local model root
     */
    static public Publisher
    publish(final Root local) {
        class PublisherX extends Struct implements Publisher, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            run(final String name, final Object value) throws Collision {
                if (name.startsWith(".")) { throw new Collision(); }
                for (int i = name.length(); i-- != 0;) {
                    if (disallowed.indexOf(name.charAt(i)) != -1) {
                        throw new Collision();
                    }
                }
                final Token x = new Token();
                if (x != local.fetch(x, name)) { throw new Collision(); }
                local.store(name, value);
            }
        }
        return new PublisherX();
    }

    /**
     * {@link Root} key for the master crypto secret
     */
    static private final String secretName = ".secret";
    
    /**
     * cipher block size in octets
     */
    static private final int blockSize = 128 / Byte.SIZE;

    /**
     * Initializes a graph for secret key access.
     * @param local local model root
     * @throws Collision    secret key could not be added
     */
    static public void
    initialize(final Root local) throws Collision {
        final SecureRandom prng = (SecureRandom)local.fetch(null, Root.prng);
        final byte[] secret = new byte[blockSize];
        prng.nextBytes(secret);
        local.store(secretName, ByteArray.array(secret));
    }
    
    /**
     * password prefix
     */
    static private final String prefix = "key.";

    /**
     * Constructs a reference exporter.
     * @param local model root
     */
    static public Exporter
    export(final Root local) {
        final Heap heap = (Heap)local.fetch(null, Root.heap);
        final ByteArray secret = (ByteArray)local.fetch(null, secretName);
        final AESEncryptor encrypt = new AESEncryptor(secret.toByteArray());
        class ExporterX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public String
            run(final Object object) {
                final long address = heap.locate(object);
                final byte[] plaintext = new byte[blockSize];
                for (int i=Long.SIZE/Byte.SIZE, s=0; 0 != i--; s += Byte.SIZE) {
                    plaintext[i] = (byte)(address >> s);
                }
                final byte[] encrypted = new byte[blockSize];
                encrypt.run(plaintext, 0, encrypted, 0);
                final String key = prefix + Base32.encode(encrypted); 
                return object instanceof Brand.Local
                    ? key
                : (object instanceof Record || object instanceof Volatile ||
                        !(Eventual.promised(object) instanceof Fulfilled)
                    ? "./?src=." : "./") + "#" + key; 
            }
        }
        return new ExporterX();
    }
    
    private final Root local;
    
    private final Heap heap;
    private final AESDecryptor decrypt;

    /**
     * Constructs an instance.
     * @param local model root
     */
    public
    Exports(final Root local) {
        this.local = local;
        
        heap = (Heap)local.fetch(null, Root.heap);
        final ByteArray secret = (ByteArray)local.fetch(null, secretName);
        decrypt = new AESDecryptor(secret.toByteArray()); 
    }
    
    /**
     * Fetches an exported reference.
     * @param name  name to lookup
     * @return bound value
     * @throws NullPointerException <code>name</code> is not bound
     */
    public Object
    use(final String name) throws NullPointerException {
        if (!name.startsWith(prefix)) {
            if (name.startsWith(".")) { throw new NullPointerException(); }
            
            final Token pumpkin = new Token();
            final Object r = local.fetch(pumpkin, name);
            if (pumpkin == r) { throw new NullPointerException(); }
            return r;
        }

        final String encoded = name.substring(prefix.length());
        final byte[] ciphertext = Base32.decode(encoded);
        if (ciphertext.length != blockSize) {throw new NullPointerException();}
        final byte[] decrypted = new byte[blockSize];
        decrypt.run(ciphertext, 0, decrypted, 0);
        for (int i = Long.SIZE / Byte.SIZE; blockSize != i; ++i) {
            if (0 != decrypted[i]) { throw new NullPointerException(); }
        }
        long address = 0L;
        for (int i = 0; i != Long.SIZE / Byte.SIZE; ++i) {
            address <<= Byte.SIZE;
            address |= decrypted[i] & 0x00FF;
        }
        return heap.reference(address);
    }
    
    /**
     * Constructs a reference importer.
     * @param here      base URL for the model
     * @param exports   exported namespace
     * @param next      next importer to try
     */
    static public Importer
    connect(final String here, final Exports exports, final Importer next) {
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;

            public Object
            run(final Class<?> type, final String URL) {
                try {
                    if (URI.resolve(URL, ".").equalsIgnoreCase(here)) {
                        final String name = Path.name(URI.path(URL));
                        return exports.use("".equals(name) ? key(URL) : name);
                    }
                } catch (final Exception e) {}
                return next.run(type, URL);
            }
        }
        return new ImporterX();
    }
    
    /**
     * Extracts the key from a web-key.
     * @param URL   web-key
     * @return corresponding key
     */
    static public String
    key(final String URL) { return URI.fragment("", URL); }
    
    /**
     * Is the given web-key a pipeline web-key?
     * @param URL   web-key
     * @return <code>true</code> if a promise, else <code>false</code>
     */
    static public boolean
    isPromise(final String URL) { return null != arg(URL, "src"); }
    
    /**
     * Extracts the soure model URL from a pipeline web-key.
     * @param URL   web-key
     * @return source model URL, or <code>null</code> if <code>URL</code> is not
     *         a pipeline web-key
     */
    static public String
    src(final String URL) {
        final String src = arg(URL, "src");
        return null == src ? null : URI.resolve(URL, src);
    }

    /**
     * Extracts a web-key argument.
     * @param URL   web-key
     * @param name  parameter name
     * @return argument value, or <code>null</code> if not specified
     */
    static private String
    arg(final String URL, final String name) {
        return Query.arg(null, URI.query("", URL), name);
    }
    
    /**
     * Calculates a pipeline key.
     * @param m message key
     * @return return value key
     */
    static public String
    pipeline(final String m) {
        final byte[] key = Base32.decode(m);
        final byte[] plaintext = new byte[blockSize];
        final byte[] hash = new byte[blockSize];
        new AESEncryptor(key).run(plaintext, 0, hash, 0);
        return "pipe." + Base32.encode(hash);
    }
    
    /**
     * Constructs a web-key.
     * @param dst   target model URL
     * @param key   {@linkplain #pipeline pipeline key}
     */
    static public String
    href(final String dst, final String key) {
        return "".equals(key) ? dst : URI.resolve(dst, "#" + key);
    }
    
    /**
     * Constructs a pipeline web-key.
     * @param dst   target model URL
     * @param key   {@linkplain #pipeline pipeline key}
     * @param src   local model URL
     */
    static public String
    href(final String dst, final String key, final String src) {
        final String a = URI.relate(dst, src);
        return URI.resolve(dst,
            ("".equals(a) ? "./" : "./?src=" + URLEncoding.encode(a)) +
            "#" + key);
    }
}
