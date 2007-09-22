// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.ASCII;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.io.Content;
import org.waterken.io.open.Open;
import org.waterken.uri.Header;

/**
 * An HTTP request.
 */
public final class
Request extends Struct implements Content, Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * <code>HTTP-Version</code>
     */
    public final String version;

    /**
     * <code>Request-Line</code> <code>Method</code>
     */
    public final String method;

    /**
     * <code>Request-URI</code>
     */
    public final String URI;

    /**
     * each header: [ name =&gt; line ]
     */
    public final PowerlessArray<Header> header;

    /**
     * entity body
     */
    public final Content body;

    /**
     * Constructs an instance.
     * @param version   {@link #version}
     * @param method    {@link #method}
     * @param URI       {@link #URI}
     * @param header    {@link #header}
     * @param body      {@link #body}
     */
    public @deserializer
    Request(@name("version") final String version,
            @name("method") final String method,
            @name("URI") final String URI,
            @name("header") final PowerlessArray<Header> header,
            @name("body") final Content body) {
        this.version = version;
        this.method = method;
        this.URI = URI;
        this.header = header;
        this.body = body;
    }
    
    // org.waterken.io.Content interface

    public void
    writeTo(final OutputStream out) throws Exception {
        final Writer hout = ASCII.output(Open.output(out));

        // Output the Request-Line.
        hout.write(method);
        hout.write(" ");
        hout.write(URI);
        hout.write(" ");
        hout.write(version);
        hout.write("\r\n");

        // Output the header.
        for (final Header h : header) {
            hout.write(h.name);
            hout.write(": ");
            hout.write(h.value);
            hout.write("\r\n");
        }
        hout.write("\r\n");
        hout.flush();
        hout.close();

        // Output the entity body.
        if (null != body) { body.writeTo(out); }
    }
    
    // org.waterken.http.Request interface

    /**
     * Gets the <code>Content-Type</code>.
     */
    public String
    getContentType() { return Header.find(null, header, "Content-Type"); }
    
    /**
     * Constructs a <code>TRACE</code> response.
     */
    public Response
    trace() {
        return new Response("HTTP/1.1", "200", "OK",
            PowerlessArray.array(
                new Header("Content-Type", "message/http; charset=iso-8859-1")
            ), this);
    }
    
    /**
     * Constructs the default response.
     * @param allow comma separated list of supported HTTP methods
     */
    public Response
    respond(final String allow) {
        return "OPTIONS".equals(method)
            ? new Response("HTTP/1.1", "204", "OK",
                PowerlessArray.array(
                    new Header("Allow", allow)
                ), null)
        : new Response("HTTP/1.1", "405", "Method Not Allowed",
            PowerlessArray.array(
                new Header("Allow", allow),
                new Header("Content-Length", "0")
            ), null);
    }
}
