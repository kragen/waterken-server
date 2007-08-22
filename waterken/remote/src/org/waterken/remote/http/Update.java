// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.remote.http;

/**
 * A {@link Message} with side-effects.
 */
abstract class
Update extends Message {
    static private final long serialVersionUID = 1L;

    /**
     * Constructs an instance.
     * @param id    {@link #id}
     */
    Update(final int id) {
        super(id);
    }
}
