// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;

/**
 * A return from a {@linkplain Eventual#spawn vat creation}.
 */
public class
Vat<T> extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * destruct the vat
     * <p>
     * call like: <code>destruct.run(null)</code>
     * </p>
     */
    public final Receiver<?> destruct;

    /**
     * object created by the vat's maker
     */
    public final T root;

    /**
     * Constructs an instance.
     * @param destruct  {@link #destruct}
     * @param root      {@link #root}
     */
    public @deserializer
    Vat(@name("destruct") final Receiver<?> destruct,
        @name("root") final T root) {
        this.destruct = destruct;
        this.root = root;
    }
}
