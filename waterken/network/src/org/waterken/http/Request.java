// Copyright 2002-2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.http;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.Writer;

import org.joe_e.Powerless;
import org.joe_e.Struct;
import org.joe_e.array.ByteArray;
import org.joe_e.array.PowerlessArray;
import org.joe_e.charset.ASCII;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.waterken.io.open.Open;
import org.waterken.uri.Header;
import org.waterken.uri.URI;

/**
 * An HTTP Request-Line and headers.
 */
public final class
Request extends Struct implements Powerless, Record, Serializable {
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
    public final String uri;

    /**
     * each header: [ name =&gt; line ]
     */
    public final PowerlessArray<Header> headers;

    /**
     * Constructs an instance.
     * @param version   {@link #version}
     * @param method    {@link #method}
     * @param uri       {@link #uri}
     * @param headers   {@link #headers}
     * @param body      {@link #body}
     */
    public @deserializer
    Request(@name("version") final String version,
            @name("method") final String method,
            @name("uri") final String uri,
            @name("headers") final PowerlessArray<Header> headers) {
        this.version = version;
        this.method = method;
        this.uri = uri;
        this.headers = headers;
    }
    
    // org.waterken.http.Request interface
    
    /**
     * Reconstructs the absolute request URI.
     * @param scheme    expected URI scheme
     */
    public String
    getAbsoluteRequestURI(final String scheme) {
        final String host = TokenList.find("", "Host", headers);
        return URI.resolve(URI.resolve(scheme + ":", "//" + host), uri);
    }

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
     * Outputs the encoding of the message header.
     * @param out   binary output stream
     * @throws IOException  any I/O problem
     */
    public void
    writeTo(final OutputStream out) throws IOException {
        final Writer hout = ASCII.output(Open.output(out));

        // output the Request-Line
        hout.write(method);
        hout.write(" ");
        hout.write(uri);
        hout.write(" ");
        hout.write(version);
        hout.write("\r\n");

        // output the header
        for (final Header header : headers) {
            hout.write(header.name);
            hout.write(": ");
            hout.write(header.value);
            hout.write("\r\n");
        }
        hout.write("\r\n");
        hout.flush();
        hout.close();
    }

    /**
     * Calls {@link #expect expect} and then {@link #allow allow}.
     * <p>
     * Both the <code>TRACE</code> and <code>OPTIONS</code> methods will be
     * handled if they are supported. If <code>true</code> is returned, the 
     * caller need only process any remaining supported methods.
     * </p>
     * @param etag      corresponding response entity tag,
     *                  or <code>null</code> if no current entity exists
     * @param client    corresponding response processor
     * @param allow     each supported HTTP method
     * @return <code>true</code> if processing should continue,
     *         else <code>false</code>
     * @throws Exception    any problem
     */
    public boolean
    respond(final String etag, final Client client,
            final String... allow) throws Exception {
        if (!expect(client, allow)) { return false; }
        final Response failed = allow(etag);
        if (null != failed) {
            client.receive(failed, null);
            return false;
        }
        if ("OPTIONS".equals(method)) {
            client.receive(Response.options(allow), null);
            return false;
        }
        return true;
    }
    
    /**
     * Handles unexpected requests.
     * <p>
     * A request processor should begin by calling this method and only
     * proceed if <code>true</code> is returned.
     * </p>
     * <p>
     * The implementation handles any <code>Expect</code> header.
     * </p>
     * @param respond   corresponding response processor
     * @param allow     each allowed HTTP method
     * @return <code>true</code> if processing should continue,
     *         else <code>false</code>
     * @throws Exception    any problem
     */
    public boolean
    expect(final Client client, final String... allow) throws Exception {
        
        // filter for disallowed methods
        boolean allowed = false;
        for (final String verb : allow) {
            if (method.equals(verb)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            client.receive(Response.notAllowed(allow), null);
            return false;
        }
        
        // process any Expect header
        for (final Header header : headers) {
            if (Header.equivalent("Expect", header.name)) {
                if (Header.equivalent("100-continue", header.value) &&
                    !(version.equals("HTTP/1.0") ||
                      version.startsWith("HTTP/0."))) {
                    client.receive(new Response("HTTP/1.1", "100", "Continue",
                        PowerlessArray.array(new Header[] {})), null);
                } else {
                    client.receive(new Response(
                        "HTTP/1.1", "417", "Expectation Failed",
                        PowerlessArray.array(
                            new Header("Content-Length", "0")
                        )), null);
                    return false;
                }
            }
        }

        // request has reached the final processor, so bounce a TRACE
        if ("TRACE".equals(method)) {
            /*
             * RFC 2616 seems to require that If-None-Match and If-Match also
             * apply to the TRACE request, but complying would interfere with
             * debugging of these headers. 
             */
            final Message<Response> r = trace();
            client.receive(r.head, r.body.asInputStream());
            return false;
        }
        return true;
    }

    /**
     * Handles preconditions on access to a particular resource.
     * @param etag  corresponding response entity tag,
     *              or <code>null</code> if no current entity exists
     * @return <code>null</code> if request should proceed,
     *         else failure response message
     */
    public Response
    allow(final String etag) {
        if (null != etag) {
            final String ifNoneMatch = TokenList.list("If-None-Match", headers);
            if ("*".equals(ifNoneMatch) || -1 != ifNoneMatch.indexOf(etag)) {
                if ("GET".equals(method) || "HEAD".equals(method)) {
                    return new Response("HTTP/1.1", "304", "Not Modified",
                                        PowerlessArray.array(
                                            new Header("ETag", etag)
                                        ));
                }
                return new Response("HTTP/1.1", "412", "Precondition Failed",
                                    PowerlessArray.array(
                                        new Header("Content-Length", "0")
                                    ));
            }
        }
        final String ifMatch = TokenList.list("If-Match", headers);
        if (!"".equals(ifMatch)) {
            if (null == etag || (-1 == ifMatch.indexOf(etag) &&
                                 !"*".equals(ifMatch))) {
                return new Response("HTTP/1.1", "412", "Precondition Failed",
                                    PowerlessArray.array(
                                        new Header("Content-Length", "0")
                                    ));
            }
        }
        return null;
    }
    
    /**
     * Constructs a <code>TRACE</code> response.
     */
    public Message<Response>
    trace() {
        final ByteArray.BuilderOutputStream buffer =
            ByteArray.builder().asOutputStream();
        try {
            writeTo(buffer);
        } catch (final IOException e) { throw new RuntimeException(e); }
        final ByteArray content = buffer.snapshot();
        return new Message<Response>(new Response(
            "HTTP/1.1", "200", "OK",
            PowerlessArray.array(
                new Header("Content-Length", "" + content.length()),
                new Header("Content-Type", "message/http; charset=iso-8859-1")
            )), content);
    }
}
