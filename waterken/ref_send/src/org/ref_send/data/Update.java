// Copyright 2008 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.data;

/**
 * An update interface.
 */
public interface
Update<P,R> extends Interpreted {

    /**
     * Executes the update.
     * @param name      operation name
     * @param argument  operation argument
     * @return operation return value
     */
    R run(String name, P argument);
}
