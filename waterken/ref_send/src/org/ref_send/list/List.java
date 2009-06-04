// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.list;

import static org.ref_send.promise.Eventual.near;
import static org.ref_send.promise.Eventual.ref;

import java.io.Serializable;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.joe_e.Equatable;
import org.joe_e.Struct;
import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;

/**
 * A linked list.
 * @param <T> element type
 */
public final class
List<T> implements Iterable<T>, Serializable {
    static private final long serialVersionUID = 1L;

    static private final class
    Link<T> implements Equatable, Serializable {
        static private final long serialVersionUID = 1L;

        Promise<Link<T>> next;
        T value;
    }

    /**
     * first element link
     */
    private Promise<Link<T>> first;

    /**
     * first unused link
     */
    private Link<T> last;

    /**
     * link count
     */
    private int capacity;

    /**
     * element count
     */
    private int size;

    private
    List() {
        last = new Link<T>();
        last.next = ref(last);
        first = last.next;
        capacity = 1;
        size = 0;
    }

    /**
     * Constructs a list.
     * @param <T> element type
     */
    static public <T> List<T>
    list() { return new List<T>(); }

    // java.lang.Iterable interface

    /**
     * Iterates over the values in this list.
     * @return forward iterator over this list
     */
    public final Iterator<T>
    iterator() { return new IteratorX(); }

    private final class
    IteratorX implements Iterator<T>, Serializable {
        static private final long serialVersionUID = 1L;

        private Link<T> current = near(first);

        public boolean
        hasNext() { return current != last; }

        public T
        next() {
            if (current == last) { throw new NoSuchElementException(); }
            final T r = current.value;
            current = near(current.next);
            return r;
        }

        public void
        remove() { throw new UnsupportedOperationException(); }
    }

    // org.ref_send.list.List interface

    /**
     * Is the element count zero?
     */
    public boolean
    isEmpty() { return 0 == size; }

    /**
     * Gets the element count.
     */
    public int
    getSize() { return size; }

    /**
     * Gets the front value.
     * @return front value
     * @throws NullPointerException list is empty
     */
    public T
    getFront() throws NullPointerException {
        if (0 == size) { throw new NullPointerException(); }
        return near(first).value;
    }

    /**
     * Removes the front element.
     * @return removed value
     * @throws NullPointerException list is empty
     */
    public T
    pop() throws NullPointerException {
        if (0 == size) { throw new NullPointerException(); }
        final Link<T> x = near(first);
        final T r = x.value;
        x.value = null;
        first = x.next;
        size -= 1;
        return r;
    }

    /**
     * Appends a value.
     * @param value value to append
     */
    public void
    append(final T value) {
        last.value = value;
        size += 1;
        if (capacity == size) {
            final Link<T> spare = new Link<T>();
            spare.next = last.next;
            last.next = ref(spare);
            capacity += 1;
        }
        last = near(last.next);
    }
    
    /**
     * Constructs an {@linkplain #append appender}.
     */
    public Receiver<T>
    appender() { return new Appender(); }

    private final class
    Appender extends Struct implements Receiver<T>, Serializable {
        static private final long serialVersionUID = 1L;

        public void
        apply(final T value) { append(value); }
    }
}
