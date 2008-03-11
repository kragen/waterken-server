// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import static org.joe_e.array.ConstArray.array;
import static org.joe_e.array.ConstArray.builder;
import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;

import org.joe_e.array.ArrayBuilder;
import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.var.Factory;
import org.ref_send.var.Variable;

/**
 * A {@link Menu} maker.
 */
public final class
MenuMaker {

    private
    MenuMaker() {}
    
    /**
     * Constructs an instance.
     * @param <T> value type
     * @param maxEntries    maximum number of entries allowed
     * @param factory       menu entry factory
     */
    static public <T> Menu<T>
    make(final int maxEntries, final Factory<Variable<T>> factory) {
        class MenuX implements Menu<T>, Serializable {
            static private final long serialVersionUID = 1L;

            private ConstArray<Variable<T>> editors = array();

            public Promise<ConstArray<T>>
            getSnapshot() {
                final ArrayBuilder<T> r = builder(editors.length());
                for (final Variable<T> x : editors) { r.append(x.get()); }
                return ref(r.snapshot());
            }

            public Promise<Variable<T>>
            grow() {
                final Variable<T> r = factory.run();

                if (editors.length() == maxEntries) { throw new TooMany(); }
                editors = editors.with(r);
                return ref(r);
            }
        }
        return new MenuX();
    }
}
