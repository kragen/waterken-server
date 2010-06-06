// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.shortcut;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.reflect.Type;

import org.joe_e.array.ConstArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.UTF8;
import org.joe_e.file.Filesystem;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.scope.Scope;
import org.waterken.http.Client;
import org.waterken.http.Request;
import org.waterken.http.Response;
import org.waterken.http.Server;
import org.waterken.syntax.Importer;
import org.waterken.syntax.json.JSONDeserializer;
import org.waterken.uri.Header;
import org.waterken.uri.Query;
import org.waterken.uri.URI;

/**
 * An Internet Shortcut emitter.
 */
public final class
InternetShortcuts implements Server, Serializable {
    static private final long serialVersionUID = 1L;

    private final String key;
    private final File root;
    private final String format;

    /**
     * Constructs an instance.
     * @param key       expected request key
     * @param root      root directory
     * @param format    shortcut file format
     */
    public @deserializer
    InternetShortcuts(@name("key") final String key,
                      @name("root") final File root,
                      @name("format") final String format) {
        this.key = key;
        this.root = root;
        this.format = format;
    }

    // org.waterken.http.Server interface
    
    public void
    serve(final String scheme, final Request head,
          final InputStream body, final Client client) throws Exception {        

        // further dispatch the request based on the query string
        final String query = URI.query("", head.uri);
        final String s = Query.arg(null, query, "s");
        final String q = Query.arg("apply", query, "q");
        if (!key.equals(s) || !"apply".equals(q)) {
            client.receive(Response.notFound(), null);
            return;
        }

        // obey any request restrictions
        if (!head.respond(null, client, "POST", "OPTIONS", "TRACE")) { return; }

        // deserialize the provided link
        class Ref {
            final String url;
            Ref(final String url) {
                this.url = url;
            }
        }
        final ConstArray<?> args = new JSONDeserializer().deserializeTuple(body,
            new Importer() {
                public Object
                apply(final String href, final String base,
                      final Type... type) throws Exception {
                    return new Ref(URI.resolve(base, href));
                }
            }, head.getAbsoluteRequestURI(scheme), null, Object.class);
        if (0 == args.length() || !(args.get(0) instanceof Scope<?>)) {
            client.receive(Response.badRequest(), null);
            return;
        }
        final Scope<?> link = (Scope<?>)args.get(0);
        final String name = link.get("name");
        final File file = Filesystem.file(root,
                name + ("Linux".equals(format) ? ".desktop" : ".url"));
        file.delete();
        final Ref href = link.get("href");
        if (null != href) {
            final Writer out = UTF8.output(Filesystem.writeNew(file));
            if ("Linux".equals(format)) {
                out.write("[Desktop Entry]\nType=Link\nName=");
                out.write(name);
                out.write("\nURL=");
                out.write(href.url);
                out.write("\n");
            } else {
                out.write("[InternetShortcut]\r\nURL=");
                out.write(href.url);
                out.write("\r\n");
            }
            out.flush();
            out.close();
        }
        
        // acknowledge the request
        client.receive(new Response("HTTP/1.1", "204", "OK",
            PowerlessArray.array(new Header[] {})), null);
    }
}
