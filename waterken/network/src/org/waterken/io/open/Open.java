// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.io.open;

import java.io.IOException;
import java.io.OutputStream;

/**
 * A stream that cannot be {@linkplain OutputStream#close closed} or
 * {@linkplain OutputStream#flush flushed}.
 */
public final class
Open {

    private
    Open() {}
    
    /**
     * Constructs an instance.
     * @param out   underlying output stream
     */
    static public OutputStream
    output(final OutputStream out) {
        return new OutputStream() {
            
            public void
            write(final int b) throws IOException { out.write(b); }

            public void
            write(final byte[] b) throws IOException { out.write(b); }

            public void
            write(final byte[] b,final int off,final int len)throws IOException{
                out.write(b, off, len);
            }

            public void
            flush() {}

            public void
            close() {}
        };
    }
}
