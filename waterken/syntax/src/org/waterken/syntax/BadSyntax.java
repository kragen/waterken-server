// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import org.joe_e.Powerless;
import org.joe_e.array.IntArray;
import org.joe_e.array.PowerlessArray;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals invalid syntax.
 */
public class
BadSyntax extends Exception implements Powerless {
    static private final long serialVersionUID = 1L;

    /**
     * path to the source code
     */
    public final String source;

    /**
     * location within {@link #source}
     */
    public final PowerlessArray<IntArray> span;
    
    /**
     * Constructs an instance.
     * @param source    {@link #source}
     * @param span      {@link #span}
     * @param cause     {@link #getCause}
     */
    public @deserializer
    BadSyntax(@name("source") final String source,
              @name("span") final PowerlessArray<IntArray> span,
              @name("cause") final Exception cause) {
        super("<" + source + "> " + span + " : ", cause);
        this.source = source;
        this.span = span;
    }
}
