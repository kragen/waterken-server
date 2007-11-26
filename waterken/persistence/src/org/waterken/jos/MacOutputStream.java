// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.Mac;

/**
 * Updates a MAC calculation. 
 */
class
MacOutputStream extends OutputStream {

    protected final OutputStream out;
    protected final Mac hash;
    
    MacOutputStream(final OutputStream out, final Mac hash) {
        this.out = out;
        this.hash = hash;
    }
    
    // java.io.OutputStream interface

    @Override public void
    write(final int b) throws IOException {
        out.write(b);
        hash.update((byte)b);
    }

    @Override public void
    write(final byte[] v, final int off, final int len) throws IOException {
        out.write(v, off, len);
        hash.update(v, off, len);
    }

    @Override public void
    write(final byte[] v) throws IOException {
        out.write(v);
        hash.update(v);
    }

    @Override public void
    close() throws IOException { out.close(); }

    @Override public void
    flush() throws IOException { out.flush(); }
}
