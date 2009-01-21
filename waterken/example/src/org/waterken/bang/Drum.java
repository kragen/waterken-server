// Copyright 2006 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.bang;

import org.ref_send.promise.Promise;

/**
 * Something to bang on.
 */
public interface
Drum {

    /**
     * Gets the number of hits.
     */
    Promise<Integer> getHits();

    /**
     * Bangs the drum.
     * @param beats number of beats
     * @return self reference
     */
    Drum bang(int beats);
}
