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
import org.ref_send.promise.eventual.Do;
import org.waterken.io.Content;
import org.waterken.io.open.Open;
import org.waterken.uri.Header;
import org.waterken.uri.URI;
import org.web_send.Failure;

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
    public final String URL;

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
     * @param URI       {@link #URL}
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
        this.URL = URI;
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
        hout.write(URL);
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
     * Gets the <code>Content-Length</code>.
     * @return number of bytes expected in {@link #body}, or -1 if unknown
     */
    public int
    getContentLength() {
        return Integer.parseInt(Header.find("-1", header, "Content-Length"));
    }
    
    /**
     * Gets the base URL for the {@link #body content}.
     * @param target    request URL
     */
    public String
    base(final String target) {
        final String location = Header.find(null, header, "Content-Location");
        if (null != location) { return URI.resolve(target, location); }
        return target;
    }
    
    /**
     * Does the client already have the current version of a resource?
     * @param etag  entity-tag for the current version of the resource.
     * @return <code>true</code> if the client should use its cached version,
     *         else <code>false</code>
     */
    public boolean
    hasVersion(final String etag) {
        final StringBuilder ifNoneMatch = new StringBuilder();
        for (final Header h : header) {
            if ("If-None-Match".equalsIgnoreCase(h.name)) {
                if (ifNoneMatch.length() != 0) { ifNoneMatch.append(","); }
                ifNoneMatch.append(h.value);
            }
        }
        final String cached = ifNoneMatch.toString();
        return (null!=etag && -1 != cached.indexOf(etag)) || "*".equals(cached);
    }

    /**
     * Starts processing of an HTTP request.
     * @param respond   corresponding response processor
     * @param allow     allowed HTTP methods
     * @return <code>true</code> if processing should continue
     * @throws Exception    any problem
     */
    public boolean
    allow(final Do<Response,?> respond,
         final String... allow) throws Exception {
        boolean allowed = false;
        for (final String verb : allow) {
            if (method.equals(verb)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            respond.fulfill(notAllowed(allow));
            return false;
        }
        if (!expectContinue(respond)) { return false; }
        if ("TRACE".equals(method)) {
            respond.fulfill(trace());
            return false;
        }
        if ("OPTIONS".equals(method)) {
            respond.fulfill(options(allow));
            return false;
        }
        return true;
    }
    
    /**
     * Constructs a <code>TRACE</code> response.
     */
    public Response
    trace() {
        return new Response("HTTP/1.1", "200", "OK",
            PowerlessArray.array(
                new Header("Content-Type", "message/http; charset=iso-8859-1")
            ), "HEAD".equals(method) ? null : this);
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
            ), null);
    }
    
    /**
     * Constructs a 405 Method Not Allowed response.
     * @param allow supported HTTP methods
     */
    static public Response
    notAllowed(final String... allow) {
        return new Response("HTTP/1.1", "405", "Method Not Allowed",
            PowerlessArray.array(
                new Header("Allow", TokenList.encode(allow)),
                new Header("Content-Length", "0")
            ), null);
    }
    
    /**
     * Handles an <code>Expect</code> header.
     * @param respond   corresponding response processor
     * @return <code>true</code> if processing should continue
     * @throws Exception    any problem
     */
    public boolean
    expectContinue(final Do<Response,?> respond) throws Exception {
        for (final Header h : header) {
            if ("Expect".equalsIgnoreCase(h.name)) {
                if ("100-continue".equals(h.value) &&
                    !(version.equals("HTTP/1.0") ||
                      version.startsWith("HTTP/0."))) {
                    final PowerlessArray<Header> header= PowerlessArray.array(); 
                    respond.fulfill(new Response("HTTP/1.1", "100", "Continue",
                                                 header, null));
                } else {
                    respond.reject(new Failure("417", "Expectation Failed"));
                    return false;
                }
            }
        }
        return true;
    }
}
