// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Mac;

/**
 * Updates a MAC calculation. 
 */
/* package */ final class
MacInputStream extends InputStream {

    private final Mac mac;
    private final InputStream in;
    
    MacInputStream(final Mac mac, final InputStream in) {
        this.mac = mac;
        this.in = in;
    }
    
    // java.io.InputStream interface

    @Override public int
    available() throws IOException { return in.available(); }

    @Override public void
    close() throws IOException { in.close(); }
    
    @Override public int
    read() throws IOException {
        final int r = in.read();
        if (-1 != r) { mac.update((byte)r); }
        return r;
    }

    @Override public int
    read(final byte[] b, final int off, final int len) throws IOException {
        final int r = in.read(b, off, len);
        if (-1 != r) { mac.update(b, off, r); }
        return r;
    }
}
