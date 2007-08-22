// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import java.io.Serializable;
import java.util.Iterator;

import org.joe_e.Struct;
import org.ref_send.promise.eventual.Channel;
import org.ref_send.promise.eventual.Eventual;
import org.ref_send.promise.eventual.Resolver;

/**
 * A {@link Series} maker.
 * @see "Section 19.9 'Queues' of 'Systems Programming in Concurrent Prolog';
 *       Concurrent Prolog Collected Papers Volume 2; Ehud Shapiro; 1987."
 */
public final class
Serial {

    private
    Serial() {}

    /**
     * Makes a {@link Series}.
     * @param _ eventual operator
     */
    static public <T> Series<T>
    make(final Eventual _) {
        final Channel<Element<T>> x = _.defer();
        class SeriesX implements Series<T>, Serializable {
            static private final long serialVersionUID = 1L;

            private Element<T> front_ = _.cast(Element.class, x.promise);
            private Resolver<Element<T>> back = x.resolver;

            public Iterator<T>
            iterator() { return new IteratorX(); }

            final class
            IteratorX implements Iterator<T>, Serializable {
                static private final long serialVersionUID = 1L;

                private Element<T> current_ = front_;

                public boolean
                hasNext() { return true; }

                public T
                next() {
                    final T r_ = current_.getValue();
                    current_ = current_.getNext();
                    return r_;
                }

                public void
                remove() { throw new UnsupportedOperationException(); }
            }

            public void
            produce(final T value) {
                final Channel<Element<T>> x = _.defer();
                back.fulfill(link(value, _.cast(Element.class, x.promise)));
                back = x.resolver;
            }

            public T
            consume() {
                final T r_ = front_.getValue();
                front_ = front_.getNext();
                return r_;
            }
        }
        return new SeriesX();
    }

    static private <T> Element<T>
    link(final T value, final Element<T> next) {
        class ElementX extends Struct implements Element<T>, Serializable {
            static private final long serialVersionUID = 1L;

            public T
            getValue() { return value; }

            public Element<T>
            getNext() { return next; }
        }
        return new ElementX();
    }
}
