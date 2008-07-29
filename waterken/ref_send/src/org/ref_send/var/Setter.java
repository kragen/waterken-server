// Copyright 2007 Waterken Inc. under the terms of the MIT X license
// found at http://www.opensource.org/licenses/mit-license.html
package org.ref_send.var;

import org.ref_send.promise.eventual.Receiver;

/**
 * The {@linkplain Variable#setter write} facet of a {@link Variable}.
 * @param <T> value type
 */
public interface
Setter<T> extends Receiver<T> {}
