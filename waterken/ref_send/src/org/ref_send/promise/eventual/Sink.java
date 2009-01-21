// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import java.io.Serializable;

import org.joe_e.Struct;

/**
 * Does nothing.
 */
/* package */ final class
Sink extends Struct implements Receiver<Object>, Serializable {
    static private final long serialVersionUID = 1L;

    public void
    run(final Object value) {}
}
