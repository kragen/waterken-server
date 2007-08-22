// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.web_send.graph;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.Record;
import org.ref_send.deserializer;
import org.ref_send.name;
import org.ref_send.promise.eventual.Eventual;

/**
 * The authority provided to the creator of a new model.
 */
public final class
Framework extends Struct implements Record, Serializable {
    static private final long serialVersionUID = 1L;

    /**
     * eventual operator
     */
    public final Eventual _;
    
    /**
     * exported namespace
     * <p>
     * All remote clients have {@link Namespace#use read} permission on
     * this namespace.
     * </p>
     */
    public final Namespace exports;
    
    /**
     * permission to destruct this model
     */
    public final Runnable destruct;
    
    /**
     * sub-model factory
     * <p>
     * All created models will be destructed when this model is destructed.
     * </p>
     */
    public final Host dependent;
    
    /**
     * Constructs an instance.
     * @param _         {@link #_}
     * @param exports   {@link #exports}
     * @param destruct  {@link #destruct}
     * @param dependent {@link #dependent}
     */
    public @deserializer
    Framework(@name("_") final Eventual _,
              @name("exports") final Namespace exports,
              @name("destruct") final Runnable destruct,
              @name("dependent") final Host dependent) {
        this._ = _;
        this.exports = exports;
        this.destruct = destruct;
        this.dependent = dependent;
    }
}
