// Copyright 2003-2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

/**
 * Signals an error produced while processing a transaction.
 * <p>
 * The application should cease using the {@link Model} until the cause of
 * the error has been corrected and the {@link Model} restored.
 * </p>
 */
public class
ModelError extends Error {
    static private final long serialVersionUID = 1;

    /**
     * Constructs an instance.
     * @param cause {@link #getCause}
     */
    public
    ModelError(final Exception cause) {
        super(cause);
    }
}
