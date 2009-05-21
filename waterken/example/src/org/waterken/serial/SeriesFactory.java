// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.serial;

import java.io.Serializable;

import org.joe_e.Struct;
import org.ref_send.promise.Eventual;

/**
 * A {@link Series} factory.
 */
public final class
SeriesFactory extends Struct implements Serializable {
    static private final long serialVersionUID = 1L;
    
    private final Eventual _;
    
    private
    SeriesFactory(final Eventual _) {
        this._ = _;
    }
    
    /**
     * Constructs an instance.
     * @param _ eventual operator
     */
    static public SeriesFactory
    make(final Eventual _) { return new SeriesFactory(_); }
    
    /**
     * Constructs a series.
     * @param <T>   value type
     */
    public <T> Series<T>
    makeSeries() { return Serial.make(_); }
}
