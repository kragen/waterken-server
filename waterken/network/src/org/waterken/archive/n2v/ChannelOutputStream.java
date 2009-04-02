// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.archive.n2v;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Adapts the {@link WritableByteChannel} API to the {@link OutputStream} API.
 */
/* package */ final class
ChannelOutputStream extends OutputStream {

    private final WritableByteChannel out;
    
    ChannelOutputStream(final WritableByteChannel out) {
        this.out = out;
    }
    
    public void
    write(final int b) throws IOException { write(new byte[] { (byte)b }); }

    public void
    write(final byte[] b, final int off, final int len) throws IOException {
        if (len != out.write(ByteBuffer.wrap(b, off, len))) {
            throw new IOException();
        }
    }
}
