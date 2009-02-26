// Copyright 2009 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.db;

/**
 * A {@linkplain Database#enter transaction} monitor.
 */
public interface
TransactionMonitor {

    /**
     * Generates an HTTP entity-tag identifying all the state accessed by the
     * current transaction.
     * @return entity-tag, or <code>null</code> if none
     */
    String tag();
}
