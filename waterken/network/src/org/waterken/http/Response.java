// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.Serializable;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.PowerlessArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.uri.Header;

/**
 * An HTTP Status-Line and headers.
 */
public final class
Response extends Struct implements Powerless, Record, Serializable {
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
    public final PowerlessArray<Header> headers;

    /**
     * Constructs an instance.
     * @param version   {@link #version}
     * @param status    {@link #status}
     * @param phrase    {@link #phrase}
     * @param headers   {@link #headers}
     */
    public @deserializer
    Response(@name("version") final String version,
             @name("status") final String status,
             @name("phrase") final String phrase,
             @name("headers") final PowerlessArray<Header> headers) {
        this.version = version;
        this.status = status;
        this.phrase = phrase;
        this.headers = headers;
    }
    
    // org.waterken.http.Response interface

    /**
     * Gets the <code>Content-Type</code>.
     * @return specified Media-Type, or <code>null</code> if unspecified
     */
    public String
    getContentType() { return TokenList.find(null, "Content-Type", headers); }

    /**
     * Gets the <code>Content-Length</code>.
     * @return number of bytes expected, or <code>-1</code> if unspecified
     */
    public int
    getContentLength() {
        final String sSize = TokenList.find(null, "Content-Length", headers);
        if (null == sSize) { return -1; }
        final int nSize = Integer.parseInt(sSize);
        if (nSize < 0) { throw new NumberFormatException(); }
        return nSize;
    }
}
