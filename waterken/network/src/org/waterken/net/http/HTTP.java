// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.net.http;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.waterken.http.Failure;
import org.waterken.http.TokenList;
import org.waterken.io.bounded.Bounded;
import org.waterken.uri.Header;

/**
 * 
 */
final class
HTTP {

    private
    HTTP() {}

    /**
     * TODO: change to whitelist
     */
    static void
    vet(final String disallowed, final String value) throws Exception {
        for (int i = value.length(); 0 != i--;) {
            final char c = value.charAt(i);
            if (31 >= c || 127 <= c || disallowed.indexOf(c) != -1) {
                throw new Exception("Illegal header character");
            }
        }
    }
    
    static void
    vetToken(final String token) throws Exception {
        vet(" ()<>@,;:\\\"/[]?={}", token);
    }
    
    static void
    vetHeader(final Header header) throws Exception {
        vetToken(header.name);
        vet("", header.value);
    }

    /**
     * Finds the first non-whitespace character.
     * @param line  text string to search
     * @param i     initial search position
     * @param end   end of search position
     * @return index of the first non-whitespace character, or <code>end</code>
     */
    static int
    skipSP(final String line, int i, final int end) {
        while (i != end && " \t".indexOf(line.charAt(i)) != -1) { ++i; }
        return i;
    }

    /**
     * Finds the first whitespace character.
     * @param line  text string to search
     * @param i     initial search position
     * @param end   end of search position
     * @return index of the first whitespace character, or <code>end</code>
     */
    static int
    findSP(final String line, int i, final int end) {
        while (i != end && " \t".indexOf(line.charAt(i)) == -1) { ++i; }
        return i;
    }
    
    /**
     * Reads HTTP message headers.
     * @param header    header list to fill
     * @param hin       header input stream
     * @throws IOException  any I/O problem
     */
    static void
    readHeaders(final ArrayList<Header> header,
                final LineInput hin) throws IOException {
        String line = hin.readln();
        while (!"".equals(line)) {
            final int len = line.length();

            // parse the header name
            final int endName = line.indexOf(':');
            final String name = line.substring(0, endName);

            // parse the header value
            final int beginValue = skipSP(line, endName + 1, len);
            String value = line.substring(beginValue);

            // check for continuations
            line = hin.readln();
            if (line.startsWith(" ") || line.startsWith("\t")) {
                final StringBuilder buffer = new StringBuilder();
                buffer.append(value);
                do {
                    buffer.append(line.substring(1));
                    line = hin.readln();
                } while (line.startsWith(" ") || line.startsWith("\t"));
                value = buffer.toString();
            }

            header.add(new Header(name, value));
        }
    }
    
    /**
     * Is a persistent connection indicated?
     * @param version   HTTP version
     * @param header    message headers
     * @return <code>true</code> if persistent, else <code>false</code>
     */
    static boolean
    persist(final String version, final ArrayList<Header> header) {
        final StringBuilder connectionList = new StringBuilder();
        for (final Iterator<Header> i = header.iterator(); i.hasNext();) {
            final Header h = i.next();
            if ("Connection".equalsIgnoreCase(h.name)) {
                if (0 != connectionList.length()) {connectionList.append(", ");}
                connectionList.append(h.value);
                i.remove();
            }
        }
        boolean close = false;
        boolean keepAlive = false;
        for (final String token : TokenList.decode(connectionList.toString())) {
            if ("close".equalsIgnoreCase(token)) {
                close = true;
            } else if ("keep-alive".equalsIgnoreCase(token)) {
                keepAlive = true;
            }
            for (Iterator<Header> i = header.iterator(); i.hasNext();) {
                if (i.next().name.equalsIgnoreCase(token)) { i.remove(); }
            }
        }
        return !close && (keepAlive || "HTTP/1.1".equals(version));
    }
    
    /**
     * Creates the input stream for reading a message body.
     * @param header    message headers
     * @param cin       connection input stream
     * @return message body input stream, or <code>null</code> if none
     * @throws Failure  indicates a stream format problem
     */
    static InputStream
    body(final ArrayList<Header> header, final InputStream cin) throws Failure {
        final StringBuilder encodingList = new StringBuilder();
        for (Iterator<Header> i = header.iterator(); i.hasNext();) {
            final Header h = i.next();
            if ("Transfer-Encoding".equalsIgnoreCase(h.name)) {
                if (0 != encodingList.length()) { encodingList.append(", "); }
                encodingList.append(h.value);
                i.remove();
            }
        }
        InputStream entity = null;
        final String[] encoding = TokenList.decode(encodingList.toString());
        for (int i = encoding.length; i-- != 0;) {
            if ("chunked".equalsIgnoreCase(encoding[i])) {
                if (i != encoding.length - 1) {
                    throw new Failure("400", "Bad Transfer-Encoding");
                }
                entity = new ChunkedInputStream(cin);
            } else if ("identity".equalsIgnoreCase(encoding[i])) {
            } else {
                throw new Failure("501", "Encoding Not Implemented");
            }
        }
        if (null == entity) {
            final String contentLength =
                Header.find(null, header, "Content-Length");
            if (null != contentLength) {
                final int length = Integer.parseInt(contentLength);
                entity = Bounded.input(length, cin);
            } else if (0 != encoding.length) {
                // identity encoding
                entity = cin;
            }
        }
        return entity;
    }
}
