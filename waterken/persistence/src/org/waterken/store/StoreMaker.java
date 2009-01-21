// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import java.io.File;

import org.ref_send.promise.eventual.Receiver;

/**
 * A {@link Store} maker.
 */
public interface
StoreMaker {
    
    /**
     * set of disallowed name characters: {@value}
     */
    String disallowed = ";\\/:*?<>|\"=#";

    /**
     * Constructs a {@link Store}.
     * @param sleep     authority to sleep the current thread
     * @param parent    parent folder, used to {@linkplain Store#clean delete}
     * @param dir       folder of existing state
     */
    Store run(Receiver<Long> sleep, File parent, File dir);
}
