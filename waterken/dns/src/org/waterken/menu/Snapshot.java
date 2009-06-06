// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.menu;

import java.io.Serializable;

import org.joe_e.Struct;
import org.joe_e.array.ConstArray;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A list of {@linkplain Copy copies}.
 * @param <T>   value type
 */
public class
Snapshot<T> extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * corresponding menu title
     */
    public final String title;
    
    /**
     * copy of each variable
     */
    public final ConstArray<Copy<T>> entries;
    
    /**
     * Constructs an instance.
     * @param entries
     */
    public @deserializer
    Snapshot(@name("title") final String title,
             @name("entries") final ConstArray<Copy<T>> entries) {
        this.title = title;
        this.entries = entries;
    }
}
