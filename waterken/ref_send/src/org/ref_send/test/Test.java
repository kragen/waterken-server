// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.test;

import org.ref_send.promise.Promise;

/**
 * A test suite.
 */
public interface
Test {

    /**
     * Starts a test.
     * @return promise for the test result
     */
    Promise<Boolean>
    start() throws Exception;
}
