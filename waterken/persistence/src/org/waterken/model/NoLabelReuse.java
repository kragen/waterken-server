// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

import org.ref_send.deserializer;
import org.web_send.graph.Collision;

/**
 * Signals an attempt to {@link Creator#run reuse} a model label. 
 */
public class
NoLabelReuse extends Collision {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    NoLabelReuse() {}
}
