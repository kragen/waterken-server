// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.waterken.model;

import org.ref_send.promise.eventual.Task;

/**
 * A transient side-effect of a {@link Model#enter transaction}.
 */
public interface
Effect extends Task {}
