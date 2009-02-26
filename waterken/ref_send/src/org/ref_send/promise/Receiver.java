// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

/**
 * A notification receiver.
 * <p>
 * This interface should be reused in any "fire-and-forget"-like pattern where
 * the caller is simply sending a notification to the callee. For example, this
 * interface should be used when implementing the Observer pattern.
 * </p>
 * @param <T> value type
 */
public interface
Receiver<T> {

    /**
     * Receives a notification.
     * @param value any additional details about the notification
     */
    void run(T value);
}
