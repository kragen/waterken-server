// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import org.waterken.model.ModelError;

/**
 * Signals a missing file.
 */
class
MissingState extends ModelError {
    private static final long serialVersionUID = 1L;

    /**
     * The id of the missing file.
     */
    final int d;

    /**
     * Constructs an instance.
     * @param cause The cause for the error.
     */
    MissingState(final Exception cause, final int d) {
        super(cause);
        this.d = d;
    }
}
