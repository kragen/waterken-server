// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import static org.ref_send.promise.Fulfilled.ref;

import java.io.Serializable;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.var.Factory;

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
    make(final int maxEntries, final Factory<T> factory) {
        class MenuX implements Menu<T>, Serializable {
            static private final long serialVersionUID = 1L;

            private ConstArray<T> entries = ConstArray.array();

            public Promise<ConstArray<T>>
            getEntries() { return ref(entries); }

            public Promise<T>
            grow() {
                final T r = factory.run();

                if (entries.length() == maxEntries) { throw new TooMany(); }
                entries = entries.with(r);
                return ref(r);
            }

            public void
            remove(final int index) {
                entries = entries.without(index);
            }
        }
        return new MenuX();
    }
}
