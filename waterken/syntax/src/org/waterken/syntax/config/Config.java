// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax.config;

import static org.joe_e.array.ConstArray.array;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.joe_e.file.Filesystem;
import org.waterken.syntax.Exporter;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.syntax.json.JSONSerializer;

/**
 * A folder of serialized configuration settings.
 */
public final class
Config extends Struct {

    private final File root;
    private final ClassLoader code;
    private final Importer connect;
    private final Exporter export;
    
    private final HashMap<String,Object> cache;
    
    /**
     * Constructs an instance.
     * @param root      root folder for configuration files
     * @param code      class loader for serialized objects
     * @param connect   reference importer, may be <code>null</code>
     * @param export    reference exporter, may be <code>null</code>
     */
    public
    Config(final File root, final ClassLoader code,
           final Importer connect, final Exporter export) {
        this.root = root;
        this.code = code;
        this.connect = connect;
        this.export = export;
        cache = new HashMap<String,Object>();
    }

    static private final String ext = ".json";
    
    /**
     * Reads a configuration setting.
     * @param <T>   expected value type
     * @param type  expected value type
     * @param name  setting name
     * @return setting value, or <code>null</code> if not set
     */
    public @SuppressWarnings("unchecked") <T> T
    read(final Class<?> type, final String name) {
        return (T)sub(root, "").run(type, name + ext, "file:///");
    }
    
    private Importer
    sub(final File root, final String prefix) {
        class ImporterX extends Struct implements Importer {
            public Object
            run(final Class<?> type, final String href, final String base) {
                try {
                    if (!"file:///".equals(base) || -1 != href.indexOf(':')) {
                        return connect.run(type, href, base);
                    }

                    // descend to the named file
                    File folder = root;     // sub-folder containing file
                    String path = prefix;   // path to folder from config root
                    String name = href;       // filename
                    while (true) {
                        final int i = name.indexOf('/');
                        if (-1 == i) { break; }
                        folder = Filesystem.file(folder, name.substring(0, i));
                        path += name.substring(0, i + 1);
                        name = name.substring(i + 1);
                    }
                    if ("".equals(name)) { return folder; }
                    final File file = Filesystem.file(folder, name);
                    if (!name.endsWith(ext)) { return file; }
                    if (!file.isFile()) { return null; }
                    
                    // deserialize the named object
                    final String key = path + name;
                    if (cache.containsKey(key)) { return cache.get(key); }
                    final Object r = new JSONDeserializer().run("file:///",
                        sub(folder, path), code, Filesystem.read(file),
                        ConstArray.array((Type)type)).get(0);
                    cache.put(key, r);
                    return r;
                } catch (final Exception e) { throw new Error(e); }
            }
        }
        return new ImporterX();
    }
    
    /**
     * Initializes a configuration setting.
     * @param name      setting name
     * @param value     setting value
     */
    public void
    init(final String name, final Object value) {
        try {
            final String key = name + ext;
            new JSONSerializer().run(export, array(value),
                Filesystem.writeNew(Filesystem.file(root, key)));
            cache.put(key, value);
        } catch (final Exception e) { throw new Error(e); }
    }
    
    /**
     * Creates a temporary override of a configuration setting.
     * @param name      setting name
     * @param value     transient setting value
     */
    public void
    override(final String name, final Object value) {
        final String key = name + ext;
        Filesystem.file(root, key);
        cache.put(key, value);
    }
}
