// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.IOException;
import java.io.OutputStream;

import javax.crypto.Mac;

/**
 * Updates a MAC calculation. 
 */
/* package */ final class
MacOutputStream extends OutputStream {

    private final Mac mac;
    private final OutputStream out;
    
    MacOutputStream(final Mac mac, final OutputStream out) {
        this.mac = mac;
        this.out = out;
    }
    
    // java.io.OutputStream interface

    @Override public void
    write(final int b) throws IOException {
        if (null != out) { out.write(b); }
        mac.update((byte)b);
    }

    @Override public void
    write(final byte[] v, final int off, final int len) throws IOException {
        if (null != out) { out.write(v, off, len); }
        mac.update(v, off, len);
    }

    @Override public void
    write(final byte[] v) throws IOException {
        if (null != out) { out.write(v); }
        mac.update(v);
    }

    @Override public void
    close() throws IOException { if (null != out) { out.close(); } }

    @Override public void
    flush() throws IOException { if (null != out) { out.flush(); } }
}
