// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.io.Content;
import org.waterken.uri.Header;

/**
 * An HTTP response.
 */
public final class
Response extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * <code>HTTP-Version</code>
     */
    public final String version;

    /**
     * <code>Status-Code</code>
     */
    public final String status;

    /**
     * <code>Reason-Phrase</code>
     */
    public final String phrase;

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
     * @param status    {@link #status}
     * @param phrase    {@link #phrase}
     * @param header    {@link #header}
     * @param body      {@link #body}
     */
    public @deserializer
    Response(@name("version") final String version,
             @name("status") final String status,
             @name("phrase") final String phrase,
             @name("header") final PowerlessArray<Header> header,
             @name("body") final Content body) {
        this.version = version;
        this.status = status;
        this.phrase = phrase;
        this.header = header;
        this.body = body;
    }

    // org.waterken.http.Response interface

    /**
     * Gets the <code>Content-Type</code>.
     */
    public String
    getContentType() { return Header.find(null, header, "Content-Type"); }

    /**
     * Gets the <code>Content-Length</code>.
     * @return number of bytes expected in {@link #body}, or -1 if unknown
     */
    public int
    getContentLength() {
        return Integer.parseInt(Header.find("-1", header, "Content-Length"));
    }
}
