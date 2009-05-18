// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * A log interface.
 */
public class
Log implements Serializable {
    static private final long serialVersionUID = 1L;
    
    /**
     * Constructs a log that discards all events.
     */
    public
    Log() {}
    
    /**
     * Logs a comment.
     * @param text  comment text
     */
    public void comment(String text) {}
    
    /**
     * Logs an exception.
     * @param reason    problem reason
     */
    public void problem(Exception reason) {}
    
    /**
     * Logs receipt of a message.
     * @param message   message identifier
     * @param concrete  concrete type of invocation target
     * @param method    declaration of invoked method
     */
    public void got(String message, Class<?> concrete, Method method) {}
    
    /**
     * Logs a message send.
     * @param message   sent message identifier
     */
    public void sent(String message) {}
    
    /**
     * Logs sending of a return value.
     * @param message   return message identifier
     */
    public void returned(String message) { sent(message); }

    /**
     * Logs a conditional message send.
     * @param message   message identifier
     * @param condition condition identifier
     */
    protected void sentIf(String message, String condition) { sent(message); }
    
    /**
     * Logs resolution of a promise.
     * @param condition condition identifier
     */
    protected void resolved(String condition) {}
    
    /**
     * Logs fulfillment of a promise.
     * @param condition condition identifier
     */
    protected void fulfilled(String condition) { resolved(condition); }
    
    /**
     * Logs rejection of a promise.
     * @param condition condition identifier
     */
    protected void rejected(String condition, Exception reason) {
        resolved(condition);
    }
    
    /**
     * Logs progress towards fulfillment of a promise.
     * @param condition condition identifier
     */
    protected void progressed(String condition) { resolved(condition); }
}
