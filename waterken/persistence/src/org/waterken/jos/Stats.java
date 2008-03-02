// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.jos;

import java.io.Serializable;

/**
 * Statistics about vat use.
 */
final class
Stats implements Serializable {
    static private final long serialVersionUID = 1L;

    private long changed = 0;
    private long dequeued = 0;
    
    Stats() {}
    
    public long
    getChanged() { return changed; }
    
    protected void
    incrementChanged() { ++changed; }
    
    public long
    getDequeued() { return dequeued; }
    
    protected void
    incrementDequeued() { ++dequeued; }
}
