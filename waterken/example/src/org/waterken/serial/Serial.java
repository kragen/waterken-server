// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import static org.ref_send.promise.Eventual.cast;

import java.io.Serializable;
import java.util.Iterator;

import org.ref_send.promise.Deferred;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Resolver;

/**
 * A {@link Series} maker.
 * @see "Section 19.9 'Queues' of 'Systems Programming in Concurrent Prolog';
 *       Concurrent Prolog Collected Papers Volume 2; Ehud Shapiro; 1987."
 */
public final class
Serial {
    private Serial() { /**/ }

    /**
     * Makes a {@link Series}.
     * @param _ eventual operator
     */
    static public <T> Series<T>
    make(final Eventual _) {
        /*
         * The implementation starts with a promise for the initial element,
         * from which all other promises are derived.
         */ 
        final Deferred<Element<T>> initial = _.defer();
        class SeriesX implements Series<T>, Serializable {
            static private final long serialVersionUID = 1L;

            protected Element<T> front_ = cast(Element.class, initial.promise);
            private   Resolver<Element<T>> back = initial.resolver;

            public Iterator<Promise<T>>
            iterator() {
                class IteratorX implements Iterator<Promise<T>>, Serializable {
                    static private final long serialVersionUID = 1L;

                    private Element<T> current_ = front_;

                    public boolean
                    hasNext() { return true; }  // The future is unlimited.

                    public Promise<T>
                    next() {
                        /*
                         * Produce a promise for what will be the value of the
                         * current element and hold onto an eventual reference
                         * for what will be the next element in the list, which
                         * is now the current element in the iteration order.
                         */
                        final Promise<T> r = current_.getValue();
                        current_ = current_.getNext();
                        return r;
                    }

                    public void
                    remove() { throw new UnsupportedOperationException(); }
                }
                return new IteratorX();
            }
            
            public Element<T>
            getFront() { return front_; }

            public void
            produce(final Promise<T> value) {
                /*
                 * Resolve the promise for the last element in the list with an
                 * actual element containing the provided value, and an eventual
                 * reference for what will be the next element in the list,
                 * which is now the new last element.
                 */
                final Deferred<Element<T>> x = _.defer();
                back.apply(Link.link(value, cast(Element.class, x.promise)));
                back = x.resolver;
            }

            public Promise<T>
            consume() {
                /*
                 * Produce a promise for what will be the value of the first
                 * element and hold onto an eventual reference for what will be
                 * the next element in the list, which is now the first element.
                 */
                final Promise<T> r = front_.getValue();
                front_ = front_.getNext();
                return r;
            }
        }
        return new SeriesX();
    }
}
