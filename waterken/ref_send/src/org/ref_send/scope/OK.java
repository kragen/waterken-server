// Copyright 2010 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.ref_send.scope;

/**
 * An empty object.
 */
public final class
OK {
    private OK() { /**/ }

    /**
     * An {@link OK} maker.
     */
    static public final Layout<OK> Maker = Layout.define();

    /**
     * Constructs an instance.
     */
    static public Scope<OK>
    ok() { return Maker.make(); }
}
