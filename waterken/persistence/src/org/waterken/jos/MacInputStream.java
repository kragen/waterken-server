// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.InputStream;

import javax.crypto.Mac;

/**
 * Updates a MAC calculation. 
 */
class
MacInputStream extends InputStream {

    protected final InputStream in;
    protected final Mac hash;
    
    MacInputStream(final InputStream in, final Mac hash) {
        this.in = in;
        this.hash = hash;
    }
    
    // java.io.InputStream interface

    @Override public int
    available() throws IOException { return in.available(); }

    @Override public void
    close() throws IOException { in.close(); }
    
    @Override public int
    read() throws IOException {
        final int r = in.read();
        if (-1 != r) { hash.update((byte)r); }
        return r;
    }

    @Override public int
    read(final byte[] b, final int off, final int len) throws IOException {
        final int r = in.read(b, off, len);
        if (-1 != r) { hash.update(b, off, r); }
        return r;
    }
}
