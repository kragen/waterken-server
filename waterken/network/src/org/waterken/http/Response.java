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

    /**
     * Constructs an <code>OPTIONS</code> response.
     * @param allow supported HTTP methods
     */
    static public Response
    options(final String... allow) {
        return new Response("HTTP/1.1", "204", "OK",
                            PowerlessArray.array(
                                new Header("Allow", TokenList.encode(allow))
                            ));
    }
    
    /**
     * Constructs a <code>400</code> Bad Request response.
     */
    static public Response
    badRequest() {
        return new Response("HTTP/1.1", "400", "Bad Request",
                            PowerlessArray.array(
                                new Header("Content-Length", "0")
                            ));
    }
    
    /**
     * Constructs a <code>404</code> Not Found response.
     */
    static public Response
    notFound() {
        return new Response("HTTP/1.1", "404", "Not Found",
                            PowerlessArray.array(
                                new Header("Content-Length", "0")
                            ));
    }
    
    /**
     * Constructs a <code>405</code> Method Not Allowed response.
     * @param each supported HTTP method
     */
    static public Response
    notAllowed(final String... allow) {
        return new Response("HTTP/1.1", "405", "Method Not Allowed",
                            PowerlessArray.array(
                                new Header("Allow", TokenList.encode(allow)),
                                new Header("Content-Length", "0")
                            ));
    }
    
    /**
     * Constructs a <code>410</code> Gone response.
     */
    static public Response
    gone() {
        return new Response("HTTP/1.1", "410", "Gone",
                            PowerlessArray.array(
                                new Header("Content-Length", "0")
                            ));
    }
    
    /**
     * Constructs a <code>413</code> Request Entity Too Large response.
     */
    static public Response
    tooBig() {
        return new Response("HTTP/1.1", "413", "Request Entity Too Large",
                            PowerlessArray.array(
                                new Header("Content-Length", "0")
                            ));
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
    
    /**
     * Constructs a response with an additional header.
     * @param name  header name
     * @param value header value
     */
    public Response
    with(final String name, final String value) {
        return new Response(version, status, phrase,
                            headers.with(new Header(name, value)));
    }
}
