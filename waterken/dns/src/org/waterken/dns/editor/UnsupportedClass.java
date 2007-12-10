// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.dns.editor;

import org.ref_send.deserializer;
import org.waterken.dns.Resource;

/**
 * Signals an unsupported DNS resource {@linkplain Resource#clazz class}.
 */
public class
UnsupportedClass extends UnsupportedResource {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     */
    public @deserializer
    UnsupportedClass() {}
}
