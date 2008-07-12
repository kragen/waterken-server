// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.base;

import java.io.Serializable;

import org.joe_e.Struct;
import org.waterken.syntax.Exporter;
import org.waterken.uri.URI;

/**
 * Base URI manipulation.
 */
public final class
Base {

    private
    Base() {}

    /**
     * Constructs an absolute URI exporter.
     * @param base  base URI
     * @param local local reference exporter
     */
    static public Exporter
    absolute(final String base, final Exporter local) {
        class AbsoluteX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public String
            run(final Object target) {
                return URI.resolve(base, local.run(target));
            }
        }
        return new AbsoluteX();
    }
    
    /**
     * Constructs a relative URI exporter.
     * @param base  base URI
     * @param local local reference exporter
     */
    static public Exporter
    relative(final String base, final Exporter local) {
        class RelativeX extends Struct implements Exporter, Serializable {
            static private final long serialVersionUID = 1L;

            public String
            run(final Object target){
                return URI.relate(base, local.run(target));
            }
        }
        return new RelativeX();
    }
}
