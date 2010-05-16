// Copyright 2010 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.delayed;

import org.ref_send.promise.Deferred;
import org.ref_send.promise.Promise;

/**
 * A promise factory.
 */
public interface
Forwarder {
    Promise<Deferred<Boolean>> forward();
}
