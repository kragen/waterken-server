package org.waterken.menu;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.Receiver;

/**
 * A {@link Receiver} snapshot.
 * @param <T>   value type
 */
public class
Copy<T> extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * value at time copy was produced
     */
    public final T value;
    
    /**
     * overwrite the value
     */
    public final Receiver<T> write;
    
    /**
     * Constructs an instance.
     * @param value {@link #value}
     * @param write {@link #write}
     */
    public @deserializer
    Copy(@name("value") final T value,
         @name("write") final Receiver<T> write) {
        this.value = value;
        this.write = write;
    }
}