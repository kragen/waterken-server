// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.server;

import java.io.Serializable;

import org.ref_send.var.Factory;

/**
 * A config setting accessor.
 */
final class
ReadConfig {

    private
    ReadConfig() {}

    /**
     * Constructs an instance.
     * @param <T>   value type
     * @param name  setting name
     */
    static protected <T> Factory<T>
    make(final Class<T> T, final String name) {
        class FactoryX extends Factory<T> implements Serializable {
            static private final long serialVersionUID = 1L;

            public T
            run() { return Config.read(T, name); }
        }
        return new FactoryX();
    }
}
