// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.joe_e.Powerless;

/**
 * A globally unique identifier.
 * @param <T> type of identified thing
 */
public interface
Brand<T> extends Type, Powerless {
    
    /**
     * A {@link Brand} maker.
     * @param <T> type of identified thing
     */
    static public final class
    Local<T> implements Brand<T>, Serializable {
        static private final long serialVersionUID = 1L;
        
        private
        Local() {}

        /**
         * Creates a new brand.
         * @param <T> type of identified thing
         */
        static public <T> Brand<T>
        brand() { return new Local<T>(); }
    }
}
