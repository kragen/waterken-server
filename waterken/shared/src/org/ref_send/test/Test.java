// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.test;

import org.ref_send.promise.Promise;
import org.ref_send.promise.eventual.Task;

/**
 * A test suite.
 */
public interface
Test extends Task<Promise<Boolean>> {}
