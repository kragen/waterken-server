// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.store;

import java.io.File;

import org.ref_send.promise.Promise;
import org.ref_send.promise.Receiver;

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
     * @param background    thread for background processing
     * @param parent        parent folder, for {@linkplain Store#clean cleaning}
     * @param dir           folder of existing state
     */
    Store run(Receiver<Promise<?>> background, File parent, File dir);
}
