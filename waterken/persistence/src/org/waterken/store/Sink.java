// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import java.io.OutputStream;

/**
 * An output stream that ignores all output.
 */
public final class
Sink extends OutputStream {
    
    /**
     * Constructs an instance.
     */
    public
    Sink() {}

    /**
     * Does nothing.
     */
    public void
    write(byte[] b, int off, int len) {}

    /**
     * Does nothing.
     */
    public void
    write(int b) {}
}
