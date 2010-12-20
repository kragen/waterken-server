// Copyright 2010 Waterken Inc. under the terms of the MIT X license found at
// http://www.opensource.org/licenses/mit-license.html
package org.ref_send.scope;

import org.joe_e.Powerless;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * Signals use of an unavailable name.
 */
public class Unavailable extends RuntimeException implements Powerless {
    static private final long serialVersionUID = 1L;

    /**
     * name that is unavailable
     */
    public final String name;

    /**
     * Constructs an instance.
     * @param name  {@link #name}
     */
    public @deserializer
    Unavailable(@name("name") final String name) {
      this.name = name;
    }
}
