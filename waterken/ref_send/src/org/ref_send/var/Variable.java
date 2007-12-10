// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A variable.
 * @param <T> value type
 */
public class
Variable<T> extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * read permission
     */
    public final Getter<T> getter;
    
    /**
     * write permission
     */
    public final Setter<T> setter;
    
    /**
     * assignment condition
     */
    public final Guard<T> guard;
    
    /**
     * Constructs an instance.
     * @param getter    {@link #getter}
     * @param setter    {@link #setter}
     * @param guard     {@link #guard}
     */
    public @deserializer
    Variable(@name("getter") final Getter<T> getter,
             @name("setter") final Setter<T> setter,
             @name("guard") final Guard<T> guard) {
        this.getter = getter;
        this.setter = setter;
        this.guard = guard;
    }
    
    // org.ref_send.var.Variable interface
    
    /**
     * Gets the near value.
     */
    public final T
    get() { return ((Slot<T>)getter).value; }
    
    /**
     * Sets the value.
     * @param value value to assign
     */
    public final void
    set(final T value) { setter.set(value); }

    /**
     * Constructs an instance.
     * @param <T> value type
     * @param value initial value
     */
    static public <T> Variable<T>
    var(final T value) { return var(value, null); }

    /**
     * Constructs an instance.
     * @param <T> value type
     * @param value initial value
     * @param guard assignment condition
     */
    static public <T> Variable<T>
    var(final T value, final Guard<T> guard) {
        final Variable<T> r = make(guard);
        r.setter.set(value);
        return r;
    }

    static private <T> Variable<T>
    make(final Guard<T> guard) {
        final Slot<T> m = new Slot<T>();
        class SetterX extends Struct implements Setter<T>, Serializable {
            static private final long serialVersionUID = 1L;

            public void
            set(final T value) {m.set(null!=guard ? guard.run(value) : value);}
        }
        return new Variable<T>(m, new SetterX(), guard);
    }
}
