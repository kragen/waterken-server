// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.promise.eventual;

import java.lang.reflect.Member;

/**
 * A log interface.
 */
public interface
Log {
    
    /**
     * Logs a comment.
     * @param text  comment text
     */
    void comment(String text);
    
    /**
     * Logs an exception.
     * @param reason    problem reason
     */
    void problem(Exception reason);
    
    /**
     * Logs receipt of a message.
     * @param message   message identifier
     * @param member    member that received the message
     */
    void got(String message, Member member);
    
    /**
     * Logs sending of a return value.
     * @param message   return message identifier
     */
    void returned(String message);
    
    /**
     * Logs a message send.
     * @param message   sent message identifier
     */
    void sent(String message);

    /**
     * Logs a conditional message send.
     * @param message   message identifier
     * @param condition condition identifier
     */
    void sentIf(String message, String condition);
    
    /**
     * Logs resolution of a condition.
     * @param condition condition identifier
     */
    void resolved(String condition);
}
