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
    protected final Mac mac;
    
    MacOutputStream(final OutputStream out, final Mac mac) {
        this.out = out;
        this.mac = mac;
    }
    
    // java.io.OutputStream interface

    @Override public void
    write(final int b) throws IOException {
        out.write(b);
        mac.update((byte)b);
    }

    @Override public void
    write(final byte[] v, final int off, final int len) throws IOException {
        out.write(v, off, len);
        mac.update(v, off, len);
    }

    @Override public void
    write(final byte[] v) throws IOException {
        out.write(v);
        mac.update(v);
    }

    @Override public void
    close() throws IOException { out.close(); }

    @Override public void
    flush() throws IOException { out.flush(); }
}
