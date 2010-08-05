// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.syntax;

import java.io.Serializable;

import org.joe_e.Struct;

/**
 * A {@linkplain Exporter#apply exported} value.
 *
 */
public class Export extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * object to transmit instead of the exported reference
     */
    public final Object replacement;
    
    /**
     * URI for the exported reference
     */
    public final String href;
    
    /**
     * Constructs a replacement instance.
     * @param replacement   {@link #replacement}
     */
    public
    Export(final Object replacement) {
        this.replacement = replacement;
        this.href = null;
    }
    
    /**
     * Constructs an exported link.
     * @param href  {@link #href}
     */
    public
    Export(final String href) {
        this.replacement = null;
        this.href = href;
    }
}
