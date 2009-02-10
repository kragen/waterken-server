// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import java.io.Serializable;
import java.util.Iterator;

import org.joe_e.Struct;
import org.ref_send.promise.Channel;
import org.ref_send.promise.Eventual;
import org.ref_send.promise.Resolver;
import org.ref_send.promise.Volatile;

/**
 * A {@link Series} maker.
 * @see "Section 19.9 'Queues' of 'Systems Programming in Concurrent Prolog';
 *       Concurrent Prolog Collected Papers Volume 2; Ehud Shapiro; 1987."
 */
public final class
Serial {
    private Serial() {}

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
        final Channel<Element<T>> initial = _.defer();
        class SeriesX implements Series<T>, Serializable {
            static private final long serialVersionUID = 1L;

            private Element<T> front_ = _.cast(Element.class, initial.promise);
            private Resolver<Element<T>> back = initial.resolver;

            public Iterator<Volatile<T>>
            iterator() {
                class IteratorX implements Iterator<Volatile<T>>, Serializable {
                    static private final long serialVersionUID = 1L;

                    private Element<T> current_ = front_;

                    public boolean
                    hasNext() { return true; }  // The future is unlimited.

                    public Volatile<T>
                    next() {
                        /*
                         * Produce a promise for what will be the value of the
                         * current element and hold onto an eventual reference
                         * for what will be the next element in the list, which
                         * is now the current element in the iteration order.
                         */
                        final Volatile<T> r = current_.getValue();
                        current_ = current_.getNext();
                        return r;
                    }

                    public void
                    remove() { throw new UnsupportedOperationException(); }
                }
                return new IteratorX();
            }


            public void
            produce(final Volatile<T> value) {
                /*
                 * Resolve the promise for the last element in the list with an
                 * actual element containing the provided value, and an eventual
                 * reference for what will be the next element in the list,
                 * which is now the new last element.
                 */
                final Channel<Element<T>> x = _.defer();
                back.run(link(value, _.cast(Element.class, x.promise)));
                back = x.resolver;
            }

            public Volatile<T>
            consume() {
                /*
                 * Produce a promise for what will be the value of the first
                 * element and hold onto an eventual reference for what will be
                 * the next element in the list, which is now the first element.
                 */
                final Volatile<T> r = front_.getValue();
                front_ = front_.getNext();
                return r;
            }
        }
        return new SeriesX();
    }

    /**
     * Constructs an element.
     * @param <T>   {@link Element#getValue} type
     * @param value {@link Element#getValue}
     * @param next  {@link Element#getNext}
     */
    static private <T> Element<T>
    link(final Volatile<T> value, final Element<T> next) {
        class ElementX extends Struct implements Element<T>, Serializable {
            static private final long serialVersionUID = 1L;

            public Volatile<T>
            getValue() { return value; }

            public Element<T>
            getNext() { return next; }
        }
        return new ElementX();
    }
}
