// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.joe_e.Selfless;
import org.joe_e.Struct;
import org.ref_send.brand.Brand;
import org.waterken.db.Root;
import org.waterken.syntax.Export;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.uri.URI;

/**
 * A remote {@link Brand}.
 */
public final class
GUID extends Brand<Object> implements Selfless {
    static private final long serialVersionUID = 1L;

    /**
     * globally unique absolute URI
     */
    protected final String href;
    
    protected
    GUID(final String href) {
        this.href = href;
    }
    
    /**
     * Constructs a {@link Brand} importer.
     * @param nonDeterminism    permission to access non-determinism
     * @param root              exported objects
     * @param next              next importer to try
     */
    static public Importer
    connect(final ClassLoader nonDeterminism,
            final Root root, final Importer next) {
        if (null == nonDeterminism) { throw new NullPointerException(); }
        class ImporterX extends Struct implements Importer, Serializable {
            static private final long serialVersionUID = 1L;

            public Object
            apply(final String href, final String base,
                                     final Type... type) throws Exception {
                if (!"urn".equals(URI.scheme(href))) {
                    return next.apply(href, base, type);
                }
                if ("guid".equals(URI.scheme(href.substring("urn:".length())))){
                    final String key = href.substring("urn:guid:".length());
                    if (!key.startsWith(".")) {
                        final Brand<?> local = root.fetch(null, key);
                        if (null != local) { return local; }
                    }
                }
                return new GUID(href);
            }
            
        }
        return new ImporterX();
    }
    
    /**
     * Constructs a {@link Brand} exporter.
     * @param nonDeterminism    permission to access non-determinism
     * @param root              exported objects
     * @param next              next exporter to try
     */
    static public Exporter
    export(final ClassLoader nonDeterminism,
           final Root root, final Exporter next) {
        if (null == nonDeterminism) { throw new NullPointerException(); }
        class ExporterX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public Export
            apply(final Object target) {
                if (target instanceof GUID) {
                    return new Export(((GUID)target).href);
                }
                if (target instanceof Brand<?>) {
                    return new Export("urn:guid:" + root.export(target, false));
                }
                return next.apply(target);
            }
        }
        return new ExporterX();
    }
    
    // java.lang.Object interface

    /**
     * Is the given object the same?
     * @param x compared to object
     * @return true if the same, else false
     */
    public boolean
    equals(final Object x) {
        return x instanceof GUID && href.equals(((GUID)x).href);
    }
    
    /**
     * Calculates the hash code.
     */
    public int
    hashCode() { return 0xB4A2D4EF; }
}
