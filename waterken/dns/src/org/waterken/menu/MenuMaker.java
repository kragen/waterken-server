// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import static org.ref_send.promise.Eventual.ref;
import static org.waterken.var.Variable.var;

import java.io.Serializable;

import org.joe_e.array.ConstArray;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;
import org.waterken.var.Guard;
import org.waterken.var.Variable;

/**
 * A {@link Menu} maker.
 */
public final class
MenuMaker {
    private MenuMaker() {}
    
    /**
     * Constructs an instance.
     * @param <T> value type
     * @param maxEntries    maximum number of entries allowed
     * @param prototype     prototype value
     * @param guard         {@link Variable#guard}
     */
    static public <T> Menu<T>
    make(final int maxEntries, final T prototype, final Guard<T> guard) {
        class MenuX implements Menu<T>, Serializable {
            static private final long serialVersionUID = 1L;

            private ConstArray<Variable<T>> editors =
                new ConstArray<Variable<T>>();

            public Promise<ConstArray<T>>
            getSnapshot() {
                final ConstArray.Builder<T> r =
                    ConstArray.builder(editors.length());
                for (final Variable<T> x : editors) { r.append(x.get()); }
                return ref(r.snapshot());
            }

            public Receiver<T>
            grow() {
                if (editors.length() == maxEntries) { throw new TooMany(); }
                final Variable<T> r = var(prototype, guard);
                editors = editors.with(r);
                return r;
            }
        }
        return new MenuX();
    }
}
